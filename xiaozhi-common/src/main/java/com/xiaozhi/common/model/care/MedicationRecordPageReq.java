package com.xiaozhi.common.model.care;

import com.xiaozhi.common.model.req.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "吃药执行记录分页查询")
public class MedicationRecordPageReq extends BasePageReq {

    @Schema(description = "老人ID")
    private String elderId;

    @Schema(description = "计划ID")
    private String planId;

    @Schema(description = "状态 pending/taken/missed")
    private String status;
}
