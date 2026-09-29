package com.xiaozhi.common.model.care;

import com.xiaozhi.common.model.req.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "关怀设备分页查询")
public class CareDevicePageReq extends BasePageReq {

    @Schema(description = "老人ID")
    private String elderId;

    @Schema(description = "在线状态 1/0")
    private String online;
}
