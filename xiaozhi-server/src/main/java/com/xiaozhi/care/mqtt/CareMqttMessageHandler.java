package com.xiaozhi.care.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaozhi.care.dal.mysql.dataobject.CareEventDO;
import com.xiaozhi.care.dal.mysql.dataobject.CommandLogDO;
import com.xiaozhi.care.dal.mysql.dataobject.ElderDO;
import com.xiaozhi.care.service.CareDeviceService;
import com.xiaozhi.care.service.CareEventService;
import com.xiaozhi.care.service.CommandService;
import com.xiaozhi.care.service.ElderService;
import com.xiaozhi.care.service.MedicationService;
import com.xiaozhi.care.sse.CarePushService;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 老年端 MQTT 消息处理器
 * - elder/{id}/status：在线/LWT 离线（retained）→ care_device 状态机 + device_offline 事件
 * - elder/{id}/event/{type}：按事件类型分发；msgId 幂等；关键事件 SSE 推送子女端
 *
 * 主题按 elderId 寻址；care_device.deviceId 取 elder.deviceId（未绑设备时回退 elderId），
 * 保证一台老人设备对应一行设备状态。
 */
@Slf4j
@Component
public class CareMqttMessageHandler {

    /** 心跳事件入库节流：距上次入库超过该分钟数才写一条 care_event（遥测每条都更新 care_device） */
    private static final int HEARTBEAT_PERSIST_MINUTES = 10;

    @Resource
    private MqttClientManager mqttClientManager;

    @Resource
    private ElderService elderService;

    @Resource
    private CareEventService careEventService;

    @Resource
    private CareDeviceService careDeviceService;

    @Resource
    private MedicationService medicationService;

    @Resource
    private CommandService commandService;

    @Resource
    private CarePushService carePushService;

    @PostConstruct
    public void register() {
        if (mqttClientManager.isEnabled()) {
            mqttClientManager.setMessageListener(this::onMessage);
        }
    }

    public void onMessage(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
        if (topic.endsWith("/status")) {
            handleStatus(mqttClientManager.topics().elderIdOfStatusTopic(topic), payload);
            return;
        }
        if (topic.contains("/event/")) {
            handleEvent(mqttClientManager.topics().elderIdOfEventTopic(topic),
                    mqttClientManager.topics().eventTypeOfTopic(topic), payload);
            return;
        }
        log.debug("忽略未识别的 MQTT 主题: {}", topic);
    }

    // -------------------- 状态主题（在线/LWT 离线） --------------------

    private void handleStatus(String elderId, String payload) {
        if (!StringUtils.hasText(elderId)) {
            return;
        }
        ElderDO elder = elderService.getByElderId(elderId);
        String deviceId = resolveDeviceId(elder, elderId);
        String state = extractString(payload, "state");
        boolean online = "online".equals(state);
        try {
            if (online) {
                careDeviceService.markOnline(deviceId, elderId);
            } else {
                // LWT 遗嘱或主动下线：更新状态并生成离线事件推给子女端
                careDeviceService.markOffline(deviceId);
                CareEventDO event = buildEvent(elderId, CareEventDO.TYPE_DEVICE_OFFLINE,
                        CareEventDO.SEVERITY_WARNING, "server", payload);
                if (careEventService.saveIdempotent(event)) {
                    carePushService.pushEvent(event, elder == null ? null : elder.getName());
                }
            }
        } catch (Exception e) {
            log.error("处理设备状态失败: elderId={}, {}", elderId, e.getMessage());
        }
    }

    // -------------------- 事件主题 --------------------

    private void handleEvent(String topicElderId, String topicType, String payload) {
        try {
            JsonNode root = JsonUtil.OBJECT_MAPPER.readTree(payload);
            // elderId 以主题为准（设备可能填错），type 优先取报文、回退主题
            String elderId = topicElderId;
            String type = root.path("type").asText(topicType);
            String msgId = root.path("msgId").asText(null);
            if (!StringUtils.hasText(elderId) || !StringUtils.hasText(type)) {
                log.warn("MQTT 事件缺少 elderId/type，丢弃: topic={}", topicType);
                return;
            }
            if (!StringUtils.hasText(msgId)) {
                // 无 msgId 的报文不符合协议：补一个服务端 id（仅告警日志不再重试）
                msgId = "srv-" + java.util.UUID.randomUUID();
                log.warn("MQTT 事件缺少 msgId，已补服务端ID: elderId={}, type={}", elderId, type);
            }

            ElderDO elder = elderService.getByElderId(elderId);
            if (elder == null || ElderDO.STATE_CANCELLED.equals(elder.getState())) {
                log.warn("MQTT 事件来自未知/已注销老人，丢弃: elderId={}, type={}", elderId, type);
                return;
            }
            String severity = normalizeSeverity(root.path("severity").asText(null), type);
            String source = root.path("source").asText("elder_device");
            JsonNode data = root.path("data");

            switch (type) {
                case CareEventDO.TYPE_HEARTBEAT -> handleHeartbeat(elder, msgId, severity, source, data);
                case CareEventDO.TYPE_DEVICE_STATUS -> handleDeviceStatus(elder, msgId, severity, source, data);
                case CareEventDO.TYPE_MEDICATION_TAKEN -> handleMedicationTaken(elder, msgId, source, data);
                case CareEventDO.TYPE_COMMAND_ACK -> handleCommandAck(data, payload);
                // fall / sos / medication_reminder（回执）/ 其他：统一落库 + 推送
                default -> persistAndPush(elder, msgId, type, severity, source, payload);
            }
        } catch (Exception e) {
            log.error("MQTT 事件处理异常: topic={}, {}", topicType, e.getMessage());
        }
    }

    private void handleHeartbeat(ElderDO elder, String msgId, String severity, String source, JsonNode data) {
        String deviceId = resolveDeviceId(elder, elderIdOf(elder));
        careDeviceService.updateTelemetry(deviceId,
                intOrNull(data, "battery"), intOrNull(data, "rssi"),
                textOrNull(data, "ip"), textOrNull(data, "firmware"));
        // 心跳节流：lastHeartbeat 每条都更新，care_event 只留采样
        LocalDateTime last = careEventService.lastHeartbeatEventTime(elder.getElderId());
        boolean due = last == null || last.isBefore(LocalDateTime.now().minusMinutes(HEARTBEAT_PERSIST_MINUTES));
        if (due) {
            persistAndPush(elder, msgId, CareEventDO.TYPE_HEARTBEAT, severity, source, data.toString());
        }
    }

    private void handleDeviceStatus(ElderDO elder, String msgId, String severity, String source, JsonNode data) {
        String deviceId = resolveDeviceId(elder, elderIdOf(elder));
        careDeviceService.updateTelemetry(deviceId,
                intOrNull(data, "battery"), intOrNull(data, "rssi"),
                textOrNull(data, "ip"), textOrNull(data, "firmware"));
        persistAndPush(elder, msgId, CareEventDO.TYPE_DEVICE_STATUS, severity, source, data.toString());
    }

    private void handleMedicationTaken(ElderDO elder, String msgId, String source, JsonNode data) {
        String planId = textOrNull(data, "planId");
        if (!StringUtils.hasText(planId)) {
            log.warn("medication_taken 缺少 planId: elderId={}", elder.getElderId());
            return;
        }
        LocalDateTime takenTime = LocalDateTime.now();
        boolean confirmed = medicationService.confirmTaken(msgId, planId, elder.getElderId(), takenTime, source);
        if (!confirmed) {
            return; // 重复确认，幂等丢弃
        }
        persistAndPush(elder, msgId, CareEventDO.TYPE_MEDICATION_TAKEN, CareEventDO.SEVERITY_INFO, source,
                JsonUtil.toJson(Map.of("planId", planId, "takenTime", takenTime.toString())));
    }

    private void handleCommandAck(JsonNode data, String rawPayload) {
        String ackMsgId = textOrNull(data, "msgId");
        if (!StringUtils.hasText(ackMsgId)) {
            log.warn("command_ack 缺少被确认的 msgId");
            return;
        }
        String status = "fail".equals(data.path("status").asText("ok"))
                || "failed".equals(data.path("status").asText("ok")) ? CommandLogDO.STATUS_FAILED : CommandLogDO.STATUS_ACKED;
        commandService.markAcked(ackMsgId, status, rawPayload);
    }

    // -------------------- 公共 --------------------

    private void persistAndPush(ElderDO elder, String msgId, String type, String severity,
                                String source, String payload) {
        CareEventDO event = new CareEventDO();
        event.setMsgId(msgId);
        event.setEventType(type);
        event.setElderId(elder.getElderId());
        event.setSeverity(severity);
        event.setSource(source);
        event.setPayload(payload);
        event.setStatus(CareEventDO.STATUS_UNHANDLED);
        if (!careEventService.saveIdempotent(event)) {
            return;
        }
        log.info("关怀事件: elder={}, type={}, severity={}", elder.getElderId(), type, severity);
        carePushService.pushEvent(event, elder.getName());
    }

    private CareEventDO buildEvent(String elderId, String type, String severity, String source, String payload) {
        CareEventDO event = new CareEventDO();
        event.setMsgId("srv-" + java.util.UUID.randomUUID());
        event.setEventType(type);
        event.setElderId(elderId);
        event.setSeverity(severity);
        event.setSource(source);
        event.setPayload(payload);
        event.setStatus(CareEventDO.STATUS_UNHANDLED);
        return event;
    }

    private String normalizeSeverity(String severity, String type) {
        if (CareEventDO.TYPE_FALL.equals(type) || CareEventDO.TYPE_SOS.equals(type)) {
            return CareEventDO.SEVERITY_CRITICAL; // 关键事件服务端强制升级
        }
        return switch (severity == null ? "" : severity) {
            case CareEventDO.SEVERITY_INFO, CareEventDO.SEVERITY_WARNING,
                 CareEventDO.SEVERITY_CRITICAL -> severity;
            default -> CareEventDO.SEVERITY_INFO;
        };
    }

    private String resolveDeviceId(ElderDO elder, String elderId) {
        if (elder != null && StringUtils.hasText(elder.getDeviceId())) {
            return elder.getDeviceId();
        }
        return elderId;
    }

    private String elderIdOf(ElderDO elder) {
        return elder == null ? null : elder.getElderId();
    }

    private Integer intOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) && node.path(field).isNumber() ? node.path(field).asInt() : null;
    }

    private String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText(null) : null;
    }

    private String extractString(String payload, String field) {
        try {
            JsonNode node = JsonUtil.OBJECT_MAPPER.readTree(payload);
            return node.path(field).asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
