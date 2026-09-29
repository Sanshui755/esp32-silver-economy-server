package com.xiaozhi.common.model.care;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "绑定关系更新（解绑/改主监护人）")
public class BindingUpdateReq implements Serializable {

    @NotNull(message = "绑定ID不能为空")
    @Schema(description = "绑定记录ID")
    private Integer id;

    @Schema(description = "0-解绑 1-重新绑定")
    private String state;

    @Schema(description = "是否主监护人 1/0")
    private String isPrimary;
}
