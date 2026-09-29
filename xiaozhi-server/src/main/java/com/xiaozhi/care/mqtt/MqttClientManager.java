package com.xiaozhi.care.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * MQTT 连接管理（老年关怀模块）
 * - Paho 自动重连 + connectComplete 里重订阅（Paho 重连不会自动恢复订阅）
 * - 固定 clientId + cleanSession=false：server 重启期间 QoS1 消息由 broker 补投
 * - broker 未配置（xiaozhi.mqtt.broker 为空）时整体禁用，不影响无 mosquitto 的部署
 */
@Slf4j
@Component
@EnableConfigurationProperties(MqttProperties.class)
public class MqttClientManager {

    private final MqttProperties properties;
    private final MqttTopics topics;

    /** 收到下行消息的处理器（由 CareMqttMessageHandler 注册） */
    private volatile BiConsumer<String, MqttMessage> messageListener;

    private MqttClient client;

    public MqttClientManager(MqttProperties properties) {
        this.properties = properties;
        this.topics = new MqttTopics(properties.getTopicPrefix());
    }

    public MqttTopics topics() {
        return topics;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public void setMessageListener(BiConsumer<String, MqttMessage> listener) {
        this.messageListener = listener;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("未配置 xiaozhi.mqtt.broker，老年关怀 MQTT 客户端不启用");
            return;
        }
        try {
            client = new MqttClient(properties.getBroker(), properties.getClientId(), new MemoryPersistence());
            MqttConnectOptions options = buildConnectOptions();

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    log.info("MQTT {}成功: {}，重新订阅关怀主题", reconnect ? "重连" : "连接", serverURI);
                    subscribeCareTopics();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT 连接断开，等待自动重连: {}", cause == null ? "" : cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    BiConsumer<String, MqttMessage> listener = messageListener;
                    if (listener != null) {
                        try {
                            listener.accept(topic, message);
                        } catch (Exception e) {
                            // 消息处理异常不能抛回 Paho，否则会中断整个回调线程
                            log.error("MQTT 消息处理失败: topic={}", topic, e);
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 发布确认无需处理：CommandGateway 同步等待 publish 返回
                }
            });
            client.connect(options);
        } catch (Exception e) {
            // 启动失败不阻断应用：Paho automaticReconnect 需要 connect 成功过一次，
            // 这里兜底后台重试（每 30s），直到连上为止
            log.error("MQTT 首次连接失败，将每 30 秒重试: {}", e.getMessage());
            // 虚拟线程恒为 daemon：依赖容器长期驻留，重试循环自行退出
            Thread.ofVirtual().name("mqtt-connect-retry").start(() -> retryConnect(buildConnectOptions()));
        }
    }

    private MqttConnectOptions buildConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(properties.getUsername());
        options.setPassword(properties.getPassword().toCharArray());
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(properties.getConnectTimeoutSeconds());
        options.setKeepAliveInterval(properties.getKeepAliveSeconds());
        options.setMaxInflight(100);
        return options;
    }

    private void retryConnect(MqttConnectOptions options) {
        while (true) {
            try {
                Thread.sleep(30_000);
                if (!client.isConnected()) {
                    client.connect(options);
                    return;
                }
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("MQTT 重试连接仍失败: {}", e.getMessage());
            }
        }
    }

    private void subscribeCareTopics() {
        try {
            client.subscribe(new String[]{topics.elderEventFilter(), topics.elderStatusFilter()},
                    new int[]{1, 1});
            log.info("MQTT 订阅完成: {}, {}", topics.elderEventFilter(), topics.elderStatusFilter());
        } catch (Exception e) {
            log.error("MQTT 订阅失败: {}", e.getMessage());
        }
    }

    /**
     * 发布下行命令（QoS 1，同步等待 broker PUBACK）
     *
     * @return true = broker 已确认接收
     */
    public boolean publishCommand(String elderId, String jsonPayload) {
        if (!properties.isEnabled() || client == null || !client.isConnected()) {
            log.warn("MQTT 不可用，命令未投递: elderId={}", elderId);
            return false;
        }
        try {
            MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(topics.commandTopic(elderId), message);
            return true;
        } catch (Exception e) {
            log.error("MQTT 命令发布失败: elderId={}, {}", elderId, e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            try {
                client.disconnect();
                client.close();
            } catch (Exception e) {
                log.warn("MQTT 客户端关闭异常: {}", e.getMessage());
            }
        }
    }
}
