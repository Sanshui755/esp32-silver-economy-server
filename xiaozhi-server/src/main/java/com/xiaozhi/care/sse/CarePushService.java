package com.xiaozhi.care.sse;

import com.xiaozhi.care.dal.mysql.dataobject.CareEventDO;
import com.xiaozhi.care.service.FamilyBindingService;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.user.service.UserService;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 关怀事件实时推送（管理端/子女端 Web）
 * - 本地 SseEmitter 注册表按 userId 分组
 * - 多实例扇出走 Redisson RTopic（与 dialogue 的 RedisSubscriber 同思路）
 * - 隐私：家属只收自己绑定老人的事件；管理员收全部
 */
@Slf4j
@Service
public class CarePushService {

    public static final String REDIS_TOPIC = "care:push:event";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private FamilyBindingService familyBindingService;

    @Resource
    private UserService userService;

    /** userId -> 该用户的全部 SSE 连接（多标签页） */
    private final Map<Integer, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @PostConstruct
    public void subscribeRedis() {
        RTopic topic = redissonClient.getTopic(REDIS_TOPIC);
        topic.addListener(String.class, (channel, json) -> broadcastLocal(json));
    }

    /** SSE 连接建立：注册 emitter 并立即回一条 connected */
    public SseEmitter register(Integer userId) {
        SseEmitter emitter = new SseEmitter(0L); // 不超时，断开由容器 IO 异常触发回调
        List<SseEmitter> list = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable cleanup = () -> list.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("ok", true)));
        } catch (IOException e) {
            cleanup.run();
        }
        return emitter;
    }

    /**
     * 事件落库成功后调用：序列化 → Redis 广播（各实例本地投递）
     */
    public void pushEvent(CareEventDO event, String elderName) {
        try {
            String json = JsonUtil.toJson(Map.of(
                    "eventId", event.getId(),
                    "eventType", event.getEventType(),
                    "severity", event.getSeverity(),
                    "elderId", event.getElderId(),
                    "elderName", elderName == null ? "" : elderName,
                    "payload", event.getPayload() == null ? "" : event.getPayload(),
                    "createTime", event.getCreateTime() == null ? "" : event.getCreateTime().toString()));
            redissonClient.getTopic(REDIS_TOPIC).publish(json);
        } catch (Exception e) {
            log.error("关怀事件推送失败: eventId={}", event.getId(), e);
        }
    }

    /** 本实例内投递：按接收人可见性过滤（家属只见绑定老人，管理员全见） */
    private void broadcastLocal(String json) {
        for (Map.Entry<Integer, List<SseEmitter>> entry : emitters.entrySet()) {
            Integer userId = entry.getKey();
            try {
                if (!visibleTo(userId, json)) {
                    continue;
                }
            } catch (Exception e) {
                log.warn("推送可见性判断失败，跳过 user={}: {}", userId, e.getMessage());
                continue;
            }
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name("care-event").data(json));
                } catch (Exception e) {
                    entry.getValue().remove(emitter);
                }
            }
        }
    }

    private boolean visibleTo(Integer userId, String eventJson) {
        com.xiaozhi.common.model.bo.UserBO user = userService.getBO(userId);
        if (user != null && UserBO.ADMIN_YES.equals(user.getIsAdmin())) {
            return true;
        }
        String elderId = extractElderId(eventJson);
        return familyBindingService.isBound(elderId, userId);
    }

    private String extractElderId(String json) {
        try {
            return JsonUtil.OBJECT_MAPPER.readTree(json).path("elderId").asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
