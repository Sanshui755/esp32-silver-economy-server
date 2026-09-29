package com.xiaozhi.care.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.care.dal.mysql.dataobject.CommandLogDO;
import com.xiaozhi.care.dal.mysql.dataobject.ElderDO;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationPlanDO;
import com.xiaozhi.care.model.MedicationPlanResp;
import com.xiaozhi.care.service.CommandService;
import com.xiaozhi.care.service.ElderService;
import com.xiaozhi.care.service.MedicationService;
import com.xiaozhi.common.model.care.MedicationPlanSaveReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.utils.CommonUtils;
import com.xiaozhi.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 老年端设备专用 REST（无用户会话，Device-Id=MAC 认证）。
 * <p>
 * 供固件调用：
 * <ul>
 *   <li>AI 工具 self.medication_reminder 的 list/add/remove 落到后端（与管理页同一份数据）</li>
 *   <li>AI 工具 self.medication_log 的 checkin 上报后端执行记录</li>
 *   <li>AI 查询老人的关怀档案（姓名/提醒计划）</li>
 * </ul>
 * 认证：请求头 Device-Id 必须与 elder 表中某老人的 deviceId 一致（MAC 归一化后比对）。
 */
@Slf4j
@RestController
@RequestMapping("/api/device/care")
@Tag(name = "老年关怀-设备端接口", description = "固件调用，Device-Id 认证")
public class CareDeviceApiController {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    @Resource
    private ElderService elderService;

    @Resource
    private MedicationService medicationService;

    @Resource
    private CommandService commandService;

    /** Device-Id → 老人（未绑定/非法 MAC 抛 400） */
    private ElderDO requireElder(String rawDeviceId) {
        if (!StringUtils.hasText(rawDeviceId)) {
            throw new IllegalArgumentException("缺少 Device-Id 请求头");
        }
        String deviceId = rawDeviceId.trim().replace('-', ':').toLowerCase();
        if (!CommonUtils.isMacAddressValid(deviceId)) {
            throw new IllegalArgumentException("设备ID不正确");
        }
        ElderDO elder = elderService.getByDeviceId(deviceId);
        if (elder == null) {
            throw new IllegalArgumentException("设备未绑定老人档案");
        }
        return elder;
    }

    @SaIgnore
    @GetMapping("/profile")
    @Operation(summary = "老人关怀档案（姓名 + 启用中的提醒计划）")
    public ApiResponse<Map<String, Object>> profile(@RequestHeader(value = "Device-Id", required = false) String deviceId) {
        ElderDO elder = requireElder(deviceId);
        List<MedicationPlanResp> plans = medicationService.pagePlans(1, 50, elder.getElderId(), null)
                .getRecords().stream().filter(p -> MedicationPlanDO.STATE_ENABLED.equals(p.getState()))
                .map(MedicationPlanResp::from).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("elderId", elder.getElderId());
        data.put("name", elder.getName());
        data.put("healthNote", elder.getHealthNote());
        data.put("plans", plans);
        return ApiResponse.success(data);
    }

    @SaIgnore
    @GetMapping("/plan")
    @Operation(summary = "启用中的提醒计划列表")
    public ApiResponse<List<MedicationPlanResp>> listPlans(@RequestHeader(value = "Device-Id", required = false) String deviceId) {
        ElderDO elder = requireElder(deviceId);
        List<MedicationPlanResp> plans = medicationService.pagePlans(1, 50, elder.getElderId(), null)
                .getRecords().stream().filter(p -> MedicationPlanDO.STATE_ENABLED.equals(p.getState()))
                .map(MedicationPlanResp::from).toList();
        return ApiResponse.success(plans);
    }

    @SaIgnore
    @PostMapping("/plan")
    @Operation(summary = "新增提醒计划（老人语音设置，与管理页同一份数据）")
    public ApiResponse<MedicationPlanResp> addPlan(@RequestHeader(value = "Device-Id", required = false) String deviceId,
                                                   @RequestBody MedicationPlanSaveReq req) {
        ElderDO elder = requireElder(deviceId);
        if (!StringUtils.hasText(req.getMedicine()) || req.getTakeTime() == null) {
            throw new IllegalArgumentException("药名和服药时间必填");
        }
        req.setElderId(elder.getElderId());
        req.setState(MedicationPlanDO.STATE_ENABLED);
        MedicationPlanDO plan = new MedicationPlanDO();
        plan.setElderId(elder.getElderId());
        plan.setMedicine(req.getMedicine());
        plan.setDosage(req.getDosage());
        plan.setTakeTime(req.getTakeTime());
        plan.setRepeatRule(req.getRepeatRule());
        plan.setWeekdays(req.getWeekdays());
        plan.setVoicePrompt(req.getVoicePrompt());
        plan.setState(MedicationPlanDO.STATE_ENABLED);
        // createdBy 列 NOT NULL 无默认值：管理页填登录 userId，设备端无登录用户，用 0 表示设备创建
        plan.setCreatedBy(0);
        medicationService.create(plan);
        log.info("设备端新增提醒计划: elder={}, planId={}, medicine={}", elder.getElderId(), plan.getPlanId(), plan.getMedicine());
        return ApiResponse.success(MedicationPlanResp.from(plan));
    }

    @SaIgnore
    @DeleteMapping("/plan/{planId}")
    @Operation(summary = "删除提醒计划（老人语音删除）")
    public ApiResponse<Void> deletePlan(@RequestHeader(value = "Device-Id", required = false) String deviceId,
                                        @PathVariable String planId) {
        ElderDO elder = requireElder(deviceId);
        // 固件删除时携带的是业务 ID plan_a8d03305ba59（POST /plan 响应里的 planId），
        // 不是数据库自增主键，必须按 planId 查询后再删
        MedicationPlanDO plan = medicationService.getByPlanId(planId);
        if (plan == null || !elder.getElderId().equals(plan.getElderId())) {
            throw new IllegalArgumentException("提醒计划不存在");
        }
        medicationService.delete(plan.getId());
        log.info("设备端删除提醒计划: elder={}, planId={}", elder.getElderId(), plan.getPlanId());
        return ApiResponse.success();
    }

    @SaIgnore
    @PostMapping("/plan/taken")
    @Operation(summary = "上报已服药（老人语音打卡，msgId 幂等）")
    public ApiResponse<Void> reportTaken(@RequestHeader(value = "Device-Id", required = false) String deviceId,
                                         @RequestBody Map<String, String> body) {
        ElderDO elder = requireElder(deviceId);
        String planId = body.get("planId");
        String medicine = body.get("medicine");
        String msgId = body.getOrDefault("msgId", "dev-" + System.currentTimeMillis());
        if (!StringUtils.hasText(planId) && !StringUtils.hasText(medicine)) {
            throw new IllegalArgumentException("planId 或 medicine 必填其一");
        }
        if (!StringUtils.hasText(planId)) {
            // 按药名找该老人最近的启用计划
            final String medName = medicine;
            var allPlans = medicationService.pagePlans(1, 50, elder.getElderId(), null).getRecords();
            var matched = allPlans.stream().filter(p -> medName.equals(p.getMedicine())).toList();
            if (matched.isEmpty()) {
                throw new IllegalArgumentException("没有找到药名为「" + medName + "」的提醒计划");
            }
            planId = matched.get(0).getPlanId();
        } else {
            MedicationPlanDO plan = medicationService.getByPlanId(planId);
            if (plan == null || !elder.getElderId().equals(plan.getElderId())) {
                throw new IllegalArgumentException("提醒计划不存在");
            }
            medicine = plan.getMedicine();
        }
        boolean ok = medicationService.confirmTaken(msgId, planId, elder.getElderId(),
                java.time.LocalDateTime.now(), "elder_device");
        log.info("设备端上报服药: elder={}, planId={}, medicine={}, 新确认={}", elder.getElderId(), planId, medicine, ok);
        return ApiResponse.success();
    }

    /**
     * 拉取关怀留言（家属通过后台下发的 care_message 命令）。
     * 设备轮询本接口代替 MQTT 订阅：返回最近 20 条未确认留言并将其标记为 acked，
     * 设备端按 msgId 幂等去重，重复拉取不会重复展示。
     */
    @SaIgnore
    @GetMapping("/messages")
    @Operation(summary = "拉取关怀留言（固件轮询，代替 MQTT 订阅，拉取即确认）")
    public ApiResponse<List<Map<String, Object>>> messages(@RequestHeader(value = "Device-Id", required = false) String deviceId) {
        ElderDO elder = requireElder(deviceId);
        var page = commandService.page(1, 20, elder.getElderId(),
                CommandLogDO.TYPE_CARE_MESSAGE, null, null);
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (CommandLogDO cmd : page.getRecords()) {
            if (CommandLogDO.STATUS_ACKED.equals(cmd.getStatus())) {
                continue;  // 已被设备拉取过
            }
            String text = "";
            String extra = "";
            try {
                Map<String, Object> payload = JsonUtil.fromJson(cmd.getPayload(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (payload != null && payload.get("data") instanceof Map<?, ?> data) {
                    Object t = data.get("text");
                    if (t != null) {
                        text = String.valueOf(t);
                    }
                    Object e = data.get("extra");
                    if (e != null) {
                        extra = String.valueOf(e);
                    }
                }
            } catch (Exception parseError) {
                log.warn("care_message payload 解析失败: msgId={}", cmd.getMsgId(), parseError);
            }
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("msgId", cmd.getMsgId());
            item.put("text", text);
            item.put("sender", extra.isBlank() ? "家人" : extra);
            item.put("time", cmd.getCreateTime() == null ? "" : cmd.getCreateTime().toString());
            out.add(item);
            // 拉取即确认，避免设备重复拉取
            commandService.markAcked(cmd.getMsgId(), CommandLogDO.STATUS_ACKED, null);
        }
        log.info("设备端拉取关怀留言: elder={}, 返回 {} 条", elder.getElderId(), out.size());
        return ApiResponse.success(out);
    }
}
