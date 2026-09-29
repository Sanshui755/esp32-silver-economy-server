package com.xiaozhi.care.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 吃药执行记录表 medication_record
 * 不继承 BaseDO：createTime 为毫秒精度，由数据库默认值生成。
 */
@Data
@TableName("medication_record")
public class MedicationRecordDO {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_TAKEN = "taken";
    public static final String STATUS_MISSED = "missed";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String planId;
    private String elderId;
    /** 计划服药时间 */
    private LocalDateTime plannedTime;
    /** pending/taken/missed */
    private String status;
    private LocalDateTime takenTime;
    /** elder_device/family_app */
    private String confirmSource;
    /** medication_taken 幂等键（唯一索引） */
    private String msgId;
    private LocalDateTime createTime;
}
