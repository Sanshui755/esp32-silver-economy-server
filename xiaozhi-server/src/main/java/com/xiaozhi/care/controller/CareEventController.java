package com.xiaozhi.care.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.care.app.CareEventAppService;
import com.xiaozhi.care.model.CareEventResp;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.care.CareEventPageReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 事件中心（摔倒/SOS/吃药/设备离线等）
 */
@Slf4j
@RestController
@RequestMapping("/api/care/event")
@Tag(name = "老年关怀-事件中心", description = "关怀事件查询与处理")
public class CareEventController extends BaseController {

    @Resource
    private CareEventAppService careEventAppService;

    @GetMapping
    @SaCheckPermission("system:care:event:api:list")
    @Operation(summary = "事件列表（分页，可按类型/严重度/状态筛选）")
    public ApiResponse<PageResult<CareEventResp>> list(@Valid CareEventPageReq req) {
        return ApiResponse.success(careEventAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:care:event:api:list")
    @Operation(summary = "事件详情")
    public ApiResponse<CareEventResp> detail(@PathVariable Long id) {
        return ApiResponse.success(careEventAppService.detail(id, StpUtil.getLoginIdAsInt()));
    }

    @PatchMapping("/{id}/handle")
    @SaCheckPermission("system:care:event:api:handle")
    @AuditLog(module = "老年关怀", operation = "事件标记已处理")
    @Operation(summary = "标记已处理")
    public ApiResponse<Void> handle(@PathVariable Long id) {
        careEventAppService.handle(id, StpUtil.getLoginIdAsInt());
        return ApiResponse.success();
    }
}
