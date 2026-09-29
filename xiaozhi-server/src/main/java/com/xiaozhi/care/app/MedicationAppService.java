package com.xiaozhi.care.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationPlanDO;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationRecordDO;
import com.xiaozhi.care.mqtt.CareCommandGateway;
import com.xiaozhi.care.model.MedicationPlanResp;
import com.xiaozhi.care.model.MedicationRecordResp;
import com.xiaozhi.care.service.MedicationService;
import com.xiaozhi.care.support.CareAccessService;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.care.MedicationPlanSaveReq;
import com.xiaozhi.common.model.care.MedicationRecordPageReq;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 吃药提醒编排：计划 CRUD 后同步下发 MQTT 命令，保持设备侧与库内计划一致
 */
@Service
public class MedicationAppService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    @Resource
    private MedicationService medicationService;

    @Resource
    private CareCommandGateway commandGateway;

    @Resource
    private CareAccessService access;

    public PageResult<MedicationPlanResp> pagePlans(Integer pageNo, Integer pageSize, String elderId, Integer userId) {
        IPage<MedicationPlanDO> page = medicationService.pagePlans(
                pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize,
                elderId, access.visibleElderIds(userId));
        Map<String, String> names = access.elderNameMap(
                page.getRecords().stream().map(MedicationPlanDO::getElderId).toList());
        List<MedicationPlanResp> list = page.getRecords().stream().map(p -> {
            MedicationPlanResp resp = MedicationPlanResp.from(p);
            resp.setElderName(names.get(p.getElderId()));
            return resp;
        }).toList();
        return new PageResult<>(list, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    public MedicationPlanResp create(MedicationPlanSaveReq req, Integer userId) {
        access.checkElderAccess(req.getElderId(), userId);
        MedicationPlanDO plan = new MedicationPlanDO();
        copySaveReq(req, plan);
        plan.setCreatedBy(userId);
        medicationService.create(plan);
        syncPlanToDevice(plan);
        return MedicationPlanResp.from(plan);
    }

    public MedicationPlanResp update(MedicationPlanSaveReq req, Integer userId) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("编辑时计划ID不能为空");
        }
        MedicationPlanDO existing = medicationService.getById(req.getId());
        if (existing == null) {
            throw new IllegalArgumentException("提醒计划不存在");
        }
        access.checkElderAccess(existing.getElderId(), userId);
        MedicationPlanDO plan = new MedicationPlanDO();
        plan.setId(req.getId());
        plan.setPlanId(existing.getPlanId());
        plan.setElderId(existing.getElderId());
        copySaveReq(req, plan);
        plan.setElderId(existing.getElderId());
        medicationService.update(plan);
        syncPlanToDevice(medicationService.getById(req.getId()));
        return MedicationPlanResp.from(medicationService.getById(req.getId()));
    }

    public void delete(Long id, Integer userId) {
        MedicationPlanDO existing = medicationService.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("提醒计划不存在");
        }
        access.checkElderAccess(existing.getElderId(), userId);
        medicationService.delete(id);
        // 设备侧同步删除
        commandGateway.send(existing.getElderId(), "delete_medication_plan", userId,
                access.currentFamilyId(userId),
                Map.of("planId", existing.getPlanId()));
    }

    public PageResult<MedicationRecordResp> pageRecords(MedicationRecordPageReq req, Integer userId) {
        MedicationRecordPageReq r = req == null ? new MedicationRecordPageReq() : req;
        IPage<MedicationRecordDO> page = medicationService.pageRecords(r.getPageNo(), r.getPageSize(),
                r.getElderId(), r.getPlanId(), r.getStatus(), access.visibleElderIds(userId));
        Map<String, String> names = access.elderNameMap(
                page.getRecords().stream().map(MedicationRecordDO::getElderId).toList());
        List<MedicationRecordResp> list = page.getRecords().stream().map(rec -> {
            MedicationRecordResp resp = MedicationRecordResp.from(rec);
            resp.setElderName(names.get(rec.getElderId()));
            var plan = medicationService.getByPlanId(rec.getPlanId());
            resp.setMedicine(plan == null ? null : plan.getMedicine());
            return resp;
        }).toList();
        return new PageResult<>(list, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /** 计划新增/修改后全量下发（设备按 planId upsert） */
    private void syncPlanToDevice(MedicationPlanDO plan) {
        if (plan == null) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("planId", plan.getPlanId());
        data.put("medicine", plan.getMedicine());
        data.put("dosage", plan.getDosage() == null ? "" : plan.getDosage());
        data.put("time", plan.getTakeTime() == null ? null : plan.getTakeTime().format(HHMM));
        data.put("repeat", plan.getRepeatRule());
        data.put("weekdays", plan.getWeekdays());
        data.put("voicePrompt", plan.getVoicePrompt());
        data.put("enabled", MedicationPlanDO.STATE_ENABLED.equals(plan.getState()));
        commandGateway.send(plan.getElderId(), "set_medication_plan", plan.getCreatedBy(),
                null, data);
    }

    private void copySaveReq(MedicationPlanSaveReq req, MedicationPlanDO plan) {
        plan.setElderId(req.getElderId());
        plan.setMedicine(req.getMedicine());
        plan.setDosage(req.getDosage());
        plan.setTakeTime(req.getTakeTime());
        plan.setRepeatRule(req.getRepeatRule());
        plan.setWeekdays(req.getWeekdays());
        plan.setVoicePrompt(req.getVoicePrompt());
        plan.setState(req.getState());
    }
}
