package com.xiaozhi.common.model.care;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;

@Data
@Schema(description = "吃药提醒计划新增/编辑（id 为空=新增）")
public class MedicationPlanSaveReq implements Serializable {

    @Schema(description = "计划ID，编辑时必填")
    private Long id;

    @NotBlank(message = "老人ID不能为空")
    @Schema(description = "老人ID")
    private String elderId;

    @NotBlank(message = "药名不能为空")
    @Schema(description = "药名")
    private String medicine;

    @Schema(description = "用量(如 1片)")
    private String dosage;

    @NotNull(message = "服药时间不能为空")
    @Schema(description = "服药时间 HH:mm:ss")
    private LocalTime takeTime;

    @Schema(description = "重复规则 daily/weekdays/custom")
    private String repeatRule;

    @Schema(description = "custom 时生效：1,3,5（1=周一）")
    private String weekdays;

    @Schema(description = "TTS 播报文案")
    private String voicePrompt;

    @Schema(description = "1-启用 0-停用")
    private String state;
}
