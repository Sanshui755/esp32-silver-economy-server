package com.xiaozhi.common.model.care;

import com.xiaozhi.common.model.req.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "老人分页查询")
public class ElderPageReq extends BasePageReq {

    @Schema(description = "姓名(模糊)")
    private String name;

    @Schema(description = "电话(模糊)")
    private String phone;

    @Schema(description = "老人ID(精确)")
    private String elderId;
}
