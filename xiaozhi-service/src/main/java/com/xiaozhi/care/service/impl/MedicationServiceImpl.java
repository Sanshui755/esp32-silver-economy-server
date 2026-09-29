package com.xiaozhi.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationPlanDO;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationRecordDO;
import com.xiaozhi.care.dal.mysql.mapper.MedicationPlanMapper;
import com.xiaozhi.care.dal.mysql.mapper.MedicationRecordMapper;
import com.xiaozhi.care.service.MedicationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
public class MedicationServiceImpl implements MedicationService {

    @Resource
    private MedicationPlanMapper planMapper;

    @Resource
    private MedicationRecordMapper recordMapper;

    @Override
    public IPage<MedicationPlanDO> pagePlans(long pageNo, long pageSize, String elderId,
                                             Collection<String> scopeElderIds) {
        QueryWrapper<MedicationPlanDO> qw = new QueryWrapper<>();
        if (StringUtils.hasText(elderId)) {
            qw.eq("elderId", elderId);
        }
        if (scopeElderIds != null) {
            if (scopeElderIds.isEmpty()) {
                return new Page<>(pageNo, pageSize);
            }
            qw.in("elderId", scopeElderIds);
        }
        qw.orderByAsc("takeTime");
        return planMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }

    @Override
    public MedicationPlanDO getById(Long id) {
        return planMapper.selectById(id);
    }

    @Override
    public MedicationPlanDO getByPlanId(String planId) {
        return planMapper.selectOne(new QueryWrapper<MedicationPlanDO>()
                .eq("planId", planId).last("LIMIT 1"));
    }

    @Override
    public void create(MedicationPlanDO plan) {
        if (!StringUtils.hasText(plan.getPlanId())) {
            plan.setPlanId("plan_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        if (!StringUtils.hasText(plan.getRepeatRule())) {
            plan.setRepeatRule(MedicationPlanDO.REPEAT_DAILY);
        }
        if (!StringUtils.hasText(plan.getState())) {
            plan.setState(MedicationPlanDO.STATE_ENABLED);
        }
        planMapper.insert(plan);
    }

    @Override
    public void update(MedicationPlanDO plan) {
        planMapper.updateById(plan);
    }

    @Override
    public void delete(Long id) {
        planMapper.deleteById(id);
    }

    @Override
    public List<MedicationPlanDO> listEnabled() {
        return planMapper.selectList(new QueryWrapper<MedicationPlanDO>()
                .eq("state", MedicationPlanDO.STATE_ENABLED));
    }

    @Override
    public boolean recordExists(String planId, LocalDateTime plannedTime) {
        return recordMapper.selectCount(new QueryWrapper<MedicationRecordDO>()
                .eq("planId", planId)
                .eq("plannedTime", plannedTime)) > 0;
    }

    @Override
    public void createRecord(MedicationRecordDO record) {
        recordMapper.insert(record);
    }

    @Override
    public boolean confirmTaken(String msgId, String planId, String elderId,
                                LocalDateTime takenTime, String confirmSource) {
        // 幂等第一道闸：同一 msgId 只处理一次
        if (recordMapper.selectCount(new QueryWrapper<MedicationRecordDO>()
                .eq("msgId", msgId)) > 0) {
            log.info("吃药确认重复投递，已丢弃: msgId={}, planId={}", msgId, planId);
            return false;
        }
        MedicationRecordDO pending = recordMapper.selectOne(new QueryWrapper<MedicationRecordDO>()
                .eq("planId", planId)
                .eq("status", MedicationRecordDO.STATUS_PENDING)
                .orderByAsc("plannedTime")
                .last("LIMIT 1"));
        if (pending != null) {
            MedicationRecordDO patch = new MedicationRecordDO();
            patch.setId(pending.getId());
            patch.setStatus(MedicationRecordDO.STATUS_TAKEN);
            patch.setTakenTime(takenTime);
            patch.setConfirmSource(confirmSource);
            patch.setMsgId(msgId);
            try {
                recordMapper.updateById(patch);
                return true;
            } catch (DuplicateKeyException e) {
                return false;
            }
        }
        // 无 pending 记录（如家属代确认）：补一条 taken 记录
        MedicationRecordDO record = new MedicationRecordDO();
        record.setPlanId(planId);
        record.setElderId(elderId);
        record.setPlannedTime(takenTime);
        record.setStatus(MedicationRecordDO.STATUS_TAKEN);
        record.setTakenTime(takenTime);
        record.setConfirmSource(confirmSource);
        record.setMsgId(msgId);
        try {
            recordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public IPage<MedicationRecordDO> pageRecords(long pageNo, long pageSize, String elderId,
                                                 String planId, String status, Collection<String> scopeElderIds) {
        QueryWrapper<MedicationRecordDO> qw = new QueryWrapper<>();
        if (StringUtils.hasText(elderId)) {
            qw.eq("elderId", elderId);
        }
        if (StringUtils.hasText(planId)) {
            qw.eq("planId", planId);
        }
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        if (scopeElderIds != null) {
            if (scopeElderIds.isEmpty()) {
                return new Page<>(pageNo, pageSize);
            }
            qw.in("elderId", scopeElderIds);
        }
        qw.orderByDesc("plannedTime");
        return recordMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }
}
