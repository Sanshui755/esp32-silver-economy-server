package com.xiaozhi.common.model.care;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "子女端命令下发")
public class CommandSendReq implements Serializable {

    @NotBlank(message = "老人ID不能为空")
    @Schema(description = "目标老人ID")
    private String elderId;

    @NotBlank(message = "命令类型不能为空")
    @Schema(description = "命令类型 care_message/query_status")
    private String commandType;

    @Schema(description = "care_message 时的关怀消息文本")
    private String message;

    @Schema(description = "附加数据(JSON 字符串，透传给设备)")
    private String extra;
}
