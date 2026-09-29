package com.xiaozhi.care.mqtt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQTT 连接配置（application.yml 的 xiaozhi.mqtt 段，值来自 .env 环境变量）
 * broker 为空 = 不启用 MQTT 客户端（无 mosquitto 的部署照常运行）
 */
@Data
@ConfigurationProperties(prefix = "xiaozhi.mqtt")
public class MqttProperties {

    /** Broker 连接串，如 tcp://mosquitto:1883；空 = 禁用 */
    private String broker;

    private String username;

    private String password;

    /** 固定 clientId + cleanSession=false：server 重启期间 QoS1 消息由 broker 补投 */
    private String clientId;

    private int connectTimeoutSeconds = 10;

    private int keepAliveSeconds = 60;

    /** 主题前缀，默认 zhiliao/v1 */
    private String topicPrefix = "zhiliao/v1";

    public boolean isEnabled() {
        return broker != null && !broker.isBlank();
    }
}
