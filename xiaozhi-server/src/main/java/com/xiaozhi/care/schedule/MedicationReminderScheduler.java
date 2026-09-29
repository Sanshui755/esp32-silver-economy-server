package com.xiaozhi.care.schedule;

import com.xiaozhi.care.dal.mysql.dataobject.CareEventDO;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationPlanDO;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationRecordDO;
import com.xiaozhi.care.mqtt.CareCommandGateway;
import com.xiaozhi.care.service.CareEventService;
import com.xiaozhi.care.service.CareDeviceService;
import com.xiaozhi.care.service.MedicationService;
import com.xiaozhi.care.service.ElderService;
import com.xiaozhi.care.dal.mysql.dataobject.ElderDO;
import com.xiaozhi.care.sse.CarePushService;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * 吃药提醒定时任务（服务端触发语义）
 * 每分钟扫描启用中的计划：takeTime 命中当前分钟且当天该触发 →
 * 1) 建 medication_record(pending)（防重复触发）
 * 2) MQTT 下行 medication_reminder 命令给老年端
 * 3) care_event(medication_reminder) 落库 + SSE 推送子女端
 * 设备/家属随后上报 medication_taken 完成闭环。
 */
@Slf4j
@Component
public class MedicationReminderScheduler {

    @Resource
    private MedicationService medicationService;

    @Resource
    private CareEventService careEventService;

    @Resource
    private CareDeviceService careDeviceService;

    @Resource
    private ElderService elderService;

    @Resource
    private CareCommandGateway commandGateway;

    @Resource
    private CarePushService carePushService;

    @Scheduled(cron = "0 * * * * *")
    public void remind() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        LocalDate today = LocalDate.now();
        for (MedicationPlanDO plan : medicationService.listEnabled()) {
            try {
                if (!plan.getTakeTime().equals(now) || !matchesToday(plan, today)) {
                    continue;
                }
                LocalDateTime plannedTime = LocalDateTime.of(today, plan.getTakeTime());
                if (medicationService.recordExists(plan.getPlanId(), plannedTime)) {
                    continue;
                }
                // 1) 执行记录（pending）
                MedicationRecordDO record = new MedicationRecordDO();
                record.setPlanId(plan.getPlanId());
                record.setElderId(plan.getElderId());
                record.setPlannedTime(plannedTime);
                record.setStatus(MedicationRecordDO.STATUS_PENDING);
                medicationService.createRecord(record);
                // 2) MQTT 下行 + 3) 事件与推送
                dispatch(plan, plannedTime);
            } catch (Exception e) {
                // 单个计划异常不影响其余计划的提醒
                log.error("吃药提醒触发失败: planId={}, {}", plan.getPlanId(), e.getMessage());
            }
        }
    }

    private void dispatch(MedicationPlanDO plan, LocalDateTime plannedTime) {
        ElderDO elder = elderService.getByElderId(plan.getElderId());
        String medicine = plan.getMedicine();
        String dosage = StringUtils.hasText(plan.getDosage()) ? plan.getDosage() : "";
        String voice = StringUtils.hasText(plan.getVoicePrompt())
                ? plan.getVoicePrompt()
                : "该吃药了：" + medicine + " " + dosage;

        Map<String, Object> data = Map.of(
                "planId", plan.getPlanId(),
                "medicine", medicine,
                "dosage", dosage,
                "time", plan.getTakeTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                "voicePrompt", voice);

        // 只有设备在线才下发（离线设备由 MQTT persistent session 补投也收不到——它根本没订阅过）
        boolean deviceOnline = isElderDeviceOnline(plan.getElderId());
        if (deviceOnline) {
            commandGateway.send(plan.getElderId(), "medication_reminder", null, null, data);
        }

        CareEventDO event = new CareEventDO();
        event.setMsgId("srv-" + UUID.randomUUID());
        event.setEventType(CareEventDO.TYPE_MEDICATION_REMINDER);
        event.setElderId(plan.getElderId());
        event.setSeverity(CareEventDO.SEVERITY_WARNING);
        event.setSource("server");
        event.setPayload(JsonUtil.toJson(Map.of(
                "planId", plan.getPlanId(),
                "medicine", medicine,
                "plannedTime", plannedTime.toString(),
                "deviceOnline", deviceOnline,
                "note", deviceOnline ? "已下发语音提醒" : "设备离线，仅记录提醒")));
        event.setStatus(CareEventDO.STATUS_UNHANDLED);
        if (careEventService.saveIdempotent(event)) {
            carePushService.pushEvent(event, elder == null ? null : elder.getName());
        }
    }

    private boolean isElderDeviceOnline(String elderId) {
        ElderDO elder = elderService.getByElderId(elderId);
        if (elder == null || !StringUtils.hasText(elder.getDeviceId())) {
            return false;
        }
        var device = careDeviceService.getByDeviceId(elder.getDeviceId());
        return device != null && "1".equals(device.getOnline());
    }

    /** 当天是否需要触发：daily 恒真；weekdays=周一~周五；custom 按 weekdays 列表（1=周一） */
    private boolean matchesToday(MedicationPlanDO plan, LocalDate today) {
        int dayValue = today.getDayOfWeek().getValue();
        String rule = plan.getRepeatRule() == null ? MedicationPlanDO.REPEAT_DAILY : plan.getRepeatRule();
        return switch (rule) {
            case MedicationPlanDO.REPEAT_DAILY -> true;
            case MedicationPlanDO.REPEAT_WEEKDAYS -> dayValue <= 5;
            case MedicationPlanDO.REPEAT_CUSTOM -> Arrays.stream(
                            (plan.getWeekdays() == null ? "" : plan.getWeekdays()).split(","))
                    .map(String::trim)
                    .anyMatch(s -> s.equals(String.valueOf(dayValue)));
            default -> false;
        };
    }
}
