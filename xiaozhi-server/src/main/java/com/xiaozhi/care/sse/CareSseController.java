package com.xiaozhi.care.sse;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 关怀事件 SSE 端点。
 * EventSource 无法带 Authorization 头，依赖 Sa-Token 的 Cookie 读取（is-read-cookie: true）；
 * 前端与后端同源（vite/nginx 代理 /api），Cookie 会自动携带。
 */
@Slf4j
@RestController
@RequestMapping("/api/care")
@Tag(name = "老年关怀-实时推送", description = "关怀事件 SSE 订阅")
public class CareSseController {

    @Resource
    private CarePushService carePushService;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅关怀事件实时推送")
    public SseEmitter subscribe() {
        Integer userId = StpUtil.getLoginIdAsInt();
        log.info("SSE 订阅: userId={}", userId);
        return carePushService.register(userId);
    }
}
