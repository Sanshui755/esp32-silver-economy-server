package com.xiaozhi.care.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 命令下发与确认记录表 command_log
 * 不继承 BaseDO：createTime 为毫秒精度，由数据库默认值生成。
 */
@Data
@TableName("command_log")
public class CommandLogDO {

    public static final String TYPE_SET_MEDICATION_PLAN = "set_medication_plan";
    public static final String TYPE_DELETE_MEDICATION_PLAN = "delete_medication_plan";
    public static final String TYPE_CARE_MESSAGE = "care_message";
    public static final String TYPE_QUERY_STATUS = "query_status";
    public static final String TYPE_MEDICATION_REMINDER = "medication_reminder";

    public static final String STATUS_SENT = "sent";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_ACKED = "acked";
    public static final String STATUS_FAILED = "failed";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 命令幂等键(UUID)，唯一索引兜底 */
    private String msgId;
    private String commandType;
    private String elderId;
    /** 发起家属(管理员下发为NULL) */
    private String familyId;
    /** 操作人(sys_user) */
    private Integer operatorId;
    /** 命令内容 JSON */
    private String payload;
    /** sent/delivered/acked/failed */
    private String status;
    private LocalDateTime ackTime;
    private String ackPayload;
    private LocalDateTime createTime;
}
