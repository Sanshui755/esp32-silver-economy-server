package com.xiaozhi.care.mqtt;

/**
 * MQTT 主题规划（老年关怀模块）
 * <pre>
 * 老年端上行事件：  zhiliao/v1/elder/{elderId}/event/{eventType}   QoS1
 * 老年端命令下行：  zhiliao/v1/elder/{elderId}/command             QoS1
 * 老年端状态(LWT)： zhiliao/v1/elder/{elderId}/status              QoS1 retained
 * 子女端通知：      zhiliao/v1/family/{familyId}/notify            （预留，默认 ACL 禁用）
 * </pre>
 */
public class MqttTopics {

    private static final String ELDER_SEGMENT = "elder";
    private static final String EVENT_SEGMENT = "event";
    private static final String COMMAND_SEGMENT = "command";
    private static final String STATUS_SEGMENT = "status";

    private final String prefix;

    public MqttTopics(String prefix) {
        String p = (prefix == null || prefix.isBlank()) ? "zhiliao/v1" : prefix;
        this.prefix = p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    /** 订阅过滤器：全部老人的事件 */
    public String elderEventFilter() {
        return prefix + "/" + ELDER_SEGMENT + "/+/event/#";
    }

    /** 订阅过滤器：全部老人的状态（在线/LWT 离线） */
    public String elderStatusFilter() {
        return prefix + "/" + ELDER_SEGMENT + "/+/status";
    }

    /** 老人命令下行主题 */
    public String commandTopic(String elderId) {
        return prefix + "/" + ELDER_SEGMENT + "/" + elderId + "/" + COMMAND_SEGMENT;
    }

    /** 事件主题 */
    public String eventTopic(String elderId, String eventType) {
        return prefix + "/" + ELDER_SEGMENT + "/" + elderId + "/" + EVENT_SEGMENT + "/" + eventType;
    }

    /** 状态主题 */
    public String statusTopic(String elderId) {
        return prefix + "/" + ELDER_SEGMENT + "/" + elderId + "/" + STATUS_SEGMENT;
    }

    /** 从事件主题解析 elderId（zhiliao/v1/elder/{id}/event/{type}），不匹配返回 null */
    public String elderIdOfEventTopic(String topic) {
        int idx = topic.lastIndexOf('/' + EVENT_SEGMENT + '/');
        if (idx < 0) return null;
        String before = topic.substring(0, idx);
        String[] parts = before.split("/");
        return parts.length >= 2 ? parts[parts.length - 1] : null;
    }

    /** 从状态主题解析 elderId（zhiliao/v1/elder/{id}/status），不匹配返回 null */
    public String elderIdOfStatusTopic(String topic) {
        String[] parts = topic.split("/");
        // [prefix..., elder, {id}, status]
        if (parts.length >= 4 && ELDER_SEGMENT.equals(parts[parts.length - 3])
                && STATUS_SEGMENT.equals(parts[parts.length - 1])) {
            return parts[parts.length - 2];
        }
        return null;
    }

    /** 从事件主题解析 eventType（zhiliao/v1/elder/{id}/event/{type}），不匹配返回 null */
    public String eventTypeOfTopic(String topic) {
        int idx = topic.lastIndexOf("/" + EVENT_SEGMENT + "/");
        return idx < 0 ? null : topic.substring(idx + EVENT_SEGMENT.length() + 2);
    }
}
