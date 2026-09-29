package com.xiaozhi.care.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.care.app.CareDeviceAppService;
import com.xiaozhi.care.model.CareDeviceResp;
import com.xiaozhi.care.model.CommandLogResp;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.care.CareDevicePageReq;
import com.xiaozhi.common.model.care.CommandLogPageReq;
import com.xiaozhi.common.model.care.CommandSendReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.server.web.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备状态 + 子女命令（关怀消息 / 状态查询 / 命令记录）
 */
@Slf4j
@RestController
@RequestMapping("/api/care/device")
@Tag(name = "老年关怀-设备与命令", description = "设备状态查询、命令下发与记录")
public class CareDeviceController extends BaseController {

    @Resource
    private CareDeviceAppService careDeviceAppService;

    @GetMapping
    @SaCheckPermission("system:care:device:api:list")
    @Operation(summary = "设备状态列表（在线/电量/最后心跳）")
    public ApiResponse<PageResult<CareDeviceResp>> list(@Valid CareDevicePageReq req) {
        return ApiResponse.success(careDeviceAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    @PostMapping("/command")
    @SaCheckPermission("system:care:command:api:send")
    @AuditLog(module = "老年关怀", operation = "下发关怀命令")
    @Operation(summary = "下发命令（care_message=关怀消息 / query_status=查询状态）")
    public ApiResponse<CommandLogResp> sendCommand(@Valid @RequestBody CommandSendReq req) {
        return ApiResponse.success(careDeviceAppService.sendCommand(req, StpUtil.getLoginIdAsInt()));
    }

    @GetMapping("/command")
    @SaCheckPermission("system:care:command:api:list")
    @Operation(summary = "命令下发记录（分页，含确认状态）")
    public ApiResponse<PageResult<CommandLogResp>> commandLogs(@Valid CommandLogPageReq req) {
        return ApiResponse.success(careDeviceAppService.commandLogs(req, StpUtil.getLoginIdAsInt()));
    }
}
