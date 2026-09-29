package com.xiaozhi.care.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.care.app.ElderAppService;
import com.xiaozhi.care.model.BindingResp;
import com.xiaozhi.care.model.ElderResp;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.care.BindingCreateReq;
import com.xiaozhi.common.model.care.BindingUpdateReq;
import com.xiaozhi.common.model.care.ElderPageReq;
import com.xiaozhi.common.model.care.ElderSaveReq;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 老人管理 + 子女绑定（老年关怀模块）
 */
@Slf4j
@RestController
@RequestMapping("/api/care/elder")
@Tag(name = "老年关怀-老人管理", description = "老人档案与子女绑定")
public class ElderController extends BaseController {

    @Resource
    private ElderAppService elderAppService;

    @GetMapping
    @SaCheckPermission("system:care:elder:api:list")
    @Operation(summary = "老人列表（分页）")
    public ApiResponse<com.xiaozhi.common.model.PageResult<ElderResp>> list(@Valid ElderPageReq req) {
        return ApiResponse.success(elderAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    @GetMapping("/{elderId}")
    @SaCheckPermission("system:care:elder:api:list")
    @Operation(summary = "老人详情")
    public ApiResponse<ElderResp> detail(@PathVariable String elderId) {
        return ApiResponse.success(elderAppService.detail(elderId, StpUtil.getLoginIdAsInt()));
    }

    @PostMapping
    @SaCheckPermission("system:care:elder:api:create")
    @AuditLog(module = "老年关怀", operation = "新增老人")
    @Operation(summary = "新增老人")
    public ApiResponse<ElderResp> create(@Valid @RequestBody ElderSaveReq req) {
        return ApiResponse.success(elderAppService.create(req, StpUtil.getLoginIdAsInt()));
    }

    @PatchMapping
    @SaCheckPermission("system:care:elder:api:update")
    @AuditLog(module = "老年关怀", operation = "编辑老人")
    @Operation(summary = "编辑老人")
    public ApiResponse<ElderResp> update(@Valid @RequestBody ElderSaveReq req) {
        return ApiResponse.success(elderAppService.update(req, StpUtil.getLoginIdAsInt()));
    }

    @DeleteMapping("/{elderId}")
    @SaCheckPermission("system:care:elder:api:delete")
    @AuditLog(module = "老年关怀", operation = "删除老人")
    @Operation(summary = "删除老人（逻辑注销）")
    public ApiResponse<Void> delete(@PathVariable String elderId) {
        elderAppService.delete(elderId, StpUtil.getLoginIdAsInt());
        return ApiResponse.success();
    }

    // -------------------- 子女绑定 --------------------

    @GetMapping("/{elderId}/binding")
    @SaCheckPermission("system:care:binding:api:list")
    @Operation(summary = "查询老人的子女绑定列表")
    public ApiResponse<List<BindingResp>> bindings(@PathVariable String elderId) {
        return ApiResponse.success(elderAppService.bindings(elderId, StpUtil.getLoginIdAsInt()));
    }

    @PostMapping("/binding")
    @SaCheckPermission("system:care:binding:api:create")
    @AuditLog(module = "老年关怀", operation = "绑定子女")
    @Operation(summary = "绑定子女")
    public ApiResponse<BindingResp> bind(@Valid @RequestBody BindingCreateReq req) {
        return ApiResponse.success(elderAppService.bind(req, StpUtil.getLoginIdAsInt()));
    }

    @PatchMapping("/binding")
    @SaCheckPermission("system:care:binding:api:delete")
    @AuditLog(module = "老年关怀", operation = "解绑/更新子女绑定")
    @Operation(summary = "解绑或更新绑定（state=0 即解绑）")
    public ApiResponse<Void> updateBinding(@Valid @RequestBody BindingUpdateReq req) {
        elderAppService.updateBinding(req, StpUtil.getLoginIdAsInt());
        return ApiResponse.success();
    }
}
