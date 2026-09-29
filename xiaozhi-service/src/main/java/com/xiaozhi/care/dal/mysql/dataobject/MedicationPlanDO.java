package com.xiaozhi.care.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 吃药提醒计划表 medication_plan
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medication_plan")
public class MedicationPlanDO extends BaseDO {

    public static final String STATE_ENABLED = "1";
    public static final String STATE_DISABLED = "0";

    public static final String REPEAT_DAILY = "daily";
    public static final String REPEAT_WEEKDAYS = "weekdays";
    public static final String REPEAT_CUSTOM = "custom";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 计划业务ID */
    private String planId;
    private String elderId;
    private String medicine;
    /** 用量(如 1片) */
    private String dosage;
    /** 服药时间 */
    private LocalTime takeTime;
    /** daily/weekdays/custom */
    private String repeatRule;
    /** custom时: 1,3,5（周一..周日，1=周一） */
    private String weekdays;
    /** TTS播报文案 */
    private String voicePrompt;
    /** 1-启用 0-停用 */
    private String state;
    /** 创建家属(sys_user) */
    private Integer createdBy;
}
