package com.cangxu.wms.device.mqtt;

import com.cangxu.wms.device.service.DeviceService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Optional MQTT ingress. Enable with MQTT_ENABLED=true and a running broker.
 * Payload example: {"deviceCode":"TH-A01","metric":"temp","value":22.6,"unit":"°C"}
 * AGV example:   {"deviceCode":"AGV-01","metric":"battery","value":72,"unit":"%","state":"WORKING"}
 */
@Component
public class MqttSubscriber {

    private static final Logger log = LoggerFactory.getLogger(MqttSubscriber.class);

    private final DeviceService deviceService;
    private final boolean enabled;
    private final String brokerUrl;
    private final String clientId;
    private final String username;
    private final String password;
    private final String topic;

    private MqttClient client;

    public MqttSubscriber(DeviceService deviceService,
                          @Value("${mqtt.enabled:false}") boolean enabled,
                          @Value("${mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
                          @Value("${mqtt.client-id:wms-backend}") String clientId,
                          @Value("${mqtt.username:}") String username,
                          @Value("${mqtt.password:}") String password,
                          @Value("${mqtt.topic:wms/sz01/#}") String topic) {
        this.deviceService = deviceService;
        this.enabled = enabled;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId + "-" + UUID.randomUUID().toString().substring(0, 6);
        this.username = username;
        this.password = password;
        this.topic = topic;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!enabled) {
            log.info("MQTT ingestion disabled (set MQTT_ENABLED=true to enable).");
            return;
        }
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password == null ? new char[0] : password.toCharArray());
            }
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    try {
                        client.subscribe(topic, 1);
                        log.info("MQTT connected: {} topic={} reconnect={}", serverURI, topic, reconnect);
                    } catch (Exception ex) {
                        log.warn("MQTT subscribe failed: {}", ex.getMessage());
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handle(new String(message.getPayload(), StandardCharsets.UTF_8));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            client.connect(options);
        } catch (Exception ex) {
            if (ex instanceof org.eclipse.paho.client.mqttv3.MqttException mqttException) {
                log.warn("MQTT connection failed: reason={} message={} cause={}",
                        mqttException.getReasonCode(), mqttException.getMessage(), mqttException.getCause());
            } else {
                log.warn("MQTT connection failed: {}", ex.getMessage(), ex);
            }
        }
    }

    private void handle(String payload) {
        try {
            JsonObject object = JsonParser.parseString(payload).getAsJsonObject();
            String deviceCode = object.get("deviceCode").getAsString();
            String metric = object.get("metric").getAsString();
            double value = object.get("value").getAsDouble();
            String unit = object.has("unit") ? object.get("unit").getAsString() : "";
            deviceService.ingest(deviceCode, metric, value, unit);
            if (object.has("state")) {
                deviceService.updateAgvState(deviceCode, object.get("state").getAsString(), (int) value);
            }
        } catch (Exception ex) {
            log.warn("Ignored invalid MQTT payload: {}", payload);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (Exception ignored) {
            // Best-effort shutdown.
        }
    }
}
