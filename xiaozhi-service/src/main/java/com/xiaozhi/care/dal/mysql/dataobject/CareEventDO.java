package com.xiaozhi.care.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关怀事件表 care_event（摔倒/SOS/提醒触发/设备离线等）
 * 不继承 BaseDO：createTime 为毫秒精度 datetime(3)，由数据库默认值生成。
 */
@Data
@TableName("care_event")
public class CareEventDO {

    public static final String TYPE_FALL = "fall";
    public static final String TYPE_SOS = "sos";
    public static final String TYPE_MEDICATION_REMINDER = "medication_reminder";
    public static final String TYPE_MEDICATION_TAKEN = "medication_taken";
    public static final String TYPE_HEARTBEAT = "heartbeat";
    public static final String TYPE_DEVICE_STATUS = "device_status";
    public static final String TYPE_DEVICE_OFFLINE = "device_offline";
    public static final String TYPE_COMMAND_ACK = "command_ack";

    public static final String SEVERITY_INFO = "info";
    public static final String SEVERITY_WARNING = "warning";
    public static final String SEVERITY_CRITICAL = "critical";

    public static final String STATUS_UNHANDLED = "0";
    public static final String STATUS_HANDLED = "1";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 消息幂等键(UUID)，唯一索引兜底 */
    private String msgId;
    /** 事件类型 */
    private String eventType;
    private String elderId;
    /** info/warning/critical */
    private String severity;
    /** elder_device/family_app/server */
    private String source;
    /** 事件原始数据 JSON(电量/位置/置信度) */
    private String payload;
    /** 0-未处理 1-已处理 */
    private String status;
    /** 处理人(sys_user) */
    private Integer handledBy;
    private LocalDateTime handledTime;
    private LocalDateTime createTime;
}
