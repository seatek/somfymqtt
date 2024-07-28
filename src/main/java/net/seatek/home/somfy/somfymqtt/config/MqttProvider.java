package net.seatek.home.somfy.somfymqtt.config;

import java.util.UUID;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MqttProvider {
	@Value("${mqtt.server.url}")
	private String mqttServerUrl;
	
	@Value("${mqtt.clientId:somfyMqtt1}")
	private String mqttClientId;
	
	@Bean
	public MqttClient mqttClient() {
		try {
			String publisherId = mqttClientId+"_"+UUID.randomUUID();
			MqttClient publisher = new MqttClient(mqttServerUrl, publisherId);
			MqttConnectionOptions options = new MqttConnectionOptions();
			options.setAutomaticReconnect(true);
			options.setCleanStart(true);
			options.setConnectionTimeout(10);
			options.setKeepAliveInterval(60);
			
//			options.setMaxInflight(100);
			IMqttToken token = publisher.connectWithResult(options);
			token.waitForCompletion();
			log.info(token.toString());
			return publisher;
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}
}
