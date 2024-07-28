package net.seatek.home.somfy.somfymqtt;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MqttDevice {
	private String uri;
	private String label;
	private String widgetName;
	private String uiClass;
	private Map<String,MqttDeviceState> states;

	public String toTopic() {
		return uri.substring(5);
	}
}
