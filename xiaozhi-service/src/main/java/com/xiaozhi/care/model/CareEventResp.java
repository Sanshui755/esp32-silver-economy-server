package com.xiaozhi.care.model;

import com.xiaozhi.care.dal.mysql.dataobject.CareEventDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "关怀事件")
public class CareEventResp implements Serializable {

    private Long id;
    private String msgId;
    private String eventType;
    private String elderId;

    @Schema(description = "老人姓名")
    private String elderName;

    private String severity;
    private String source;
    private String payload;
    private String status;
    private Integer handledBy;
    private LocalDateTime handledTime;
    private LocalDateTime createTime;

    public static CareEventResp from(CareEventDO d) {
        CareEventResp r = new CareEventResp();
        r.setId(d.getId());
        r.setMsgId(d.getMsgId());
        r.setEventType(d.getEventType());
        r.setElderId(d.getElderId());
        r.setSeverity(d.getSeverity());
        r.setSource(d.getSource());
        r.setPayload(d.getPayload());
        r.setStatus(d.getStatus());
        r.setHandledBy(d.getHandledBy());
        r.setHandledTime(d.getHandledTime());
        r.setCreateTime(d.getCreateTime());
        return r;
    }
}
