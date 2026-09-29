package com.xiaozhi.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationPlanDO;
import com.xiaozhi.care.dal.mysql.dataobject.MedicationRecordDO;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 吃药提醒计划与执行记录
 */
public interface MedicationService {

    IPage<MedicationPlanDO> pagePlans(long pageNo, long pageSize, String elderId,
                                      Collection<String> scopeElderIds);

    MedicationPlanDO getById(Long id);
    MedicationPlanDO getByPlanId(String planId);

    void create(MedicationPlanDO plan);
    void update(MedicationPlanDO plan);
    void delete(Long id);

    /** 全部启用中的计划（定时任务扫描用） */
    List<MedicationPlanDO> listEnabled();

    /** 该计划在该时间点是否已有执行记录（防定时任务重复触发） */
    boolean recordExists(String planId, LocalDateTime plannedTime);

    /** 新建执行记录（定时任务触发提醒时建 pending 记录） */
    void createRecord(MedicationRecordDO record);

    /**
     * 设备/家属确认吃药。
     * 幂等：msgId 已存在直接返回 false；
     * 优先更新该计划最近的 pending 记录，没有则补一条 taken 记录（家属手动确认场景）。
     */
    boolean confirmTaken(String msgId, String planId, String elderId,
                         LocalDateTime takenTime, String confirmSource);

    IPage<MedicationRecordDO> pageRecords(long pageNo, long pageSize, String elderId,
                                          String planId, String status, Collection<String> scopeElderIds);
}
