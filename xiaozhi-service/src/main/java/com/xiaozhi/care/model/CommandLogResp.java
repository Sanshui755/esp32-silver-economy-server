package com.xiaozhi.care.model;

import com.xiaozhi.care.dal.mysql.dataobject.CommandLogDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "命令下发记录")
public class CommandLogResp implements Serializable {

    private Long id;
    private String msgId;
    private String commandType;
    private String elderId;

    @Schema(description = "老人姓名")
    private String elderName;

    private String familyId;
    private Integer operatorId;
    private String payload;
    private String status;
    private LocalDateTime ackTime;
    private String ackPayload;
    private LocalDateTime createTime;

    public static CommandLogResp from(CommandLogDO d) {
        CommandLogResp r = new CommandLogResp();
        r.setId(d.getId());
        r.setMsgId(d.getMsgId());
        r.setCommandType(d.getCommandType());
        r.setElderId(d.getElderId());
        r.setFamilyId(d.getFamilyId());
        r.setOperatorId(d.getOperatorId());
        r.setPayload(d.getPayload());
        r.setStatus(d.getStatus());
        r.setAckTime(d.getAckTime());
        r.setAckPayload(d.getAckPayload());
        r.setCreateTime(d.getCreateTime());
        return r;
    }
}
