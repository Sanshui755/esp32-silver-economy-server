package com.xiaozhi.common.model.care;

import com.xiaozhi.common.model.req.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "关怀事件分页查询")
public class CareEventPageReq extends BasePageReq {

    @Schema(description = "老人ID")
    private String elderId;

    @Schema(description = "事件类型 fall/sos/medication_reminder/medication_taken/device_offline/heartbeat/device_status/command_ack")
    private String eventType;

    @Schema(description = "严重程度 info/warning/critical")
    private String severity;

    @Schema(description = "处理状态 0-未处理 1-已处理")
    private String status;
}
