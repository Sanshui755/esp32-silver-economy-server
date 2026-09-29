package com.xiaozhi.common.model.care;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "老人新增/编辑（elderId 为空=新增）")
public class ElderSaveReq implements Serializable {

    @Schema(description = "老人ID，编辑时必填")
    private String elderId;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别 1-男 0-女")
    private String gender;

    @Schema(description = "出生日期 yyyy-MM-dd")
    private LocalDate birthday;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "居住地址")
    private String address;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "绑定设备ID(care_device.deviceId)")
    private String deviceId;

    @Schema(description = "健康备注")
    private String healthNote;
}
