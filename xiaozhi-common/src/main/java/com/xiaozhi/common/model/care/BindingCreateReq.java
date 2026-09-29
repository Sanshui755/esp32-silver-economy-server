package com.xiaozhi.common.model.care;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "绑定子女到老人")
public class BindingCreateReq implements Serializable {

    @NotBlank(message = "老人ID不能为空")
    @Schema(description = "老人ID")
    private String elderId;

    @Schema(description = "家属账号(sys_user.userId)，按 userId 绑定")
    private Integer userId;

    @Schema(description = "关系 son/daughter/other")
    private String relation;

    @Schema(description = "是否主监护人 1/0")
    private String isPrimary;
}
