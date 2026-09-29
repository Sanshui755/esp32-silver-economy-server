package com.xiaozhi.common.model.care;

import com.xiaozhi.common.model.req.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "命令下发记录分页查询")
public class CommandLogPageReq extends BasePageReq {

    @Schema(description = "老人ID")
    private String elderId;

    @Schema(description = "命令类型")
    private String commandType;

    @Schema(description = "状态 sent/delivered/acked/failed")
    private String status;
}
