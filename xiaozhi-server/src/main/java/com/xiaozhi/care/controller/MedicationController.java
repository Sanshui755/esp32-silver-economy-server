package com.xiaozhi.care.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.care.app.MedicationAppService;
import com.xiaozhi.care.model.MedicationPlanResp;
import com.xiaozhi.care.model.MedicationRecordResp;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.care.MedicationPlanSaveReq;
import com.xiaozhi.common.model.care.MedicationRecordPageReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 吃药提醒（计划 CRUD 同步下发设备；执行记录来自设备确认/家属确认）
 */
@Slf4j
@RestController
@RequestMapping("/api/care/medication")
@Tag(name = "老年关怀-吃药提醒", description = "提醒计划与执行记录")
public class MedicationController extends BaseController {

    @Resource
    private MedicationAppService medicationAppService;

    @GetMapping("/plan")
    @SaCheckPermission("system:care:medication:api:list")
    @Operation(summary = "提醒计划列表（分页）")
    public ApiResponse<PageResult<MedicationPlanResp>> plans(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String elderId) {
        return ApiResponse.success(medicationAppService.pagePlans(pageNo, pageSize, elderId,
                StpUtil.getLoginIdAsInt()));
    }

    @PostMapping("/plan")
    @SaCheckPermission("system:care:medication:api:create")
    @AuditLog(module = "老年关怀", operation = "新建吃药提醒")
    @Operation(summary = "新建提醒计划（同步下发设备）")
    public ApiResponse<MedicationPlanResp> create(@Valid @RequestBody MedicationPlanSaveReq req) {
        return ApiResponse.success(medicationAppService.create(req, StpUtil.getLoginIdAsInt()));
    }

    @PatchMapping("/plan")
    @SaCheckPermission("system:care:medication:api:update")
    @AuditLog(module = "老年关怀", operation = "编辑吃药提醒")
    @Operation(summary = "编辑提醒计划（同步下发设备；state=0 即停用）")
    public ApiResponse<MedicationPlanResp> update(@Valid @RequestBody MedicationPlanSaveReq req) {
        return ApiResponse.success(medicationAppService.update(req, StpUtil.getLoginIdAsInt()));
    }

    @DeleteMapping("/plan/{id}")
    @SaCheckPermission("system:care:medication:api:delete")
    @AuditLog(module = "老年关怀", operation = "删除吃药提醒")
    @Operation(summary = "删除提醒计划（同步删除设备侧）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        medicationAppService.delete(id, StpUtil.getLoginIdAsInt());
        return ApiResponse.success();
    }

    @GetMapping("/record")
    @SaCheckPermission("system:care:medication:api:record")
    @Operation(summary = "执行记录列表（分页）")
    public ApiResponse<PageResult<MedicationRecordResp>> records(@Valid MedicationRecordPageReq req) {
        return ApiResponse.success(medicationAppService.pageRecords(req, StpUtil.getLoginIdAsInt()));
    }
}
