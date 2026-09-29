package com.xiaozhi.care.mqtt;

import com.xiaozhi.care.dal.mysql.dataobject.CommandLogDO;
import com.xiaozhi.care.service.CommandService;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 命令下行网关：构建命令报文 → command_log 幂等落库 → MQTT 发布
 * 报文结构（与协议文档一致）：
 * { msgId, version:"1.0", type, elderId, timestamp, source, data:{...} }
 */
@Slf4j
@Component
public class CareCommandGateway {

    @Resource
    private MqttClientManager mqttClientManager;

    @Resource
    private CommandService commandService;

    /**
     * 下发命令
     *
     * @param elderId    目标老人
     * @param type       命令类型（set_medication_plan / delete_medication_plan / care_message / query_status / medication_reminder）
     * @param operatorId 操作人（定时任务触发的提醒为 null）
     * @param familyId   发起家属（系统触发为 null）
     * @param data       命令数据
     * @return 落库后的命令记录（含最终状态）
     */
    public CommandLogDO send(String elderId, String type, Integer operatorId,
                             String familyId, Map<String, Object> data) {
        String msgId = UUID.randomUUID().toString();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("msgId", msgId);
        message.put("version", "1.0");
        message.put("type", type);
        message.put("elderId", elderId);
        message.put("timestamp", System.currentTimeMillis());
        message.put("source", familyId == null ? "server" : "family_app");
        message.put("data", data == null ? Map.of() : data);
        String json = JsonUtil.toJson(message);

        CommandLogDO log = new CommandLogDO();
        log.setMsgId(msgId);
        log.setCommandType(type);
        log.setElderId(elderId);
        log.setFamilyId(familyId);
        log.setOperatorId(operatorId);
        log.setPayload(json);
        boolean inserted = commandService.saveIdempotent(log);
        if (!inserted) {
            return commandService.getByMsgId(msgId);
        }
        boolean delivered = mqttClientManager.publishCommand(elderId, json);
        // Paho QoS1 publish 返回即代表 broker 已确认（PUBACK）；未连上 MQTT 则标记 failed 留待重试/排查
        CommandLogDO patch = new CommandLogDO();
        patch.setId(log.getId());
        patch.setStatus(delivered ? CommandLogDO.STATUS_DELIVERED : CommandLogDO.STATUS_FAILED);
        commandService.markAcked(msgId, patch.getStatus(), null);
        log.setStatus(patch.getStatus());
        return log;
    }
}
