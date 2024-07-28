package net.seatek.home.somfy.somfymqtt;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.somfy.tahoma.api.ExecutionApi;
import com.somfy.tahoma.invoker.ApiClient;
import com.somfy.tahoma.model.Device;
import com.somfy.tahoma.model.ExecApplyPost200Response;
import com.somfy.tahoma.model.ExecApplyPostRequest;
import com.somfy.tahoma.model.ExecApplyPostRequestActionsInner;
import com.somfy.tahoma.model.ExecApplyPostRequestActionsInnerCommandsInner;
import com.somfy.tahoma.model.State;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MqttRegistry implements IMqttMessageListener, MqttActionListener {
	@Autowired
	MqttClient mqttClient;

	Set<Device> devices = new HashSet<>();
	Map<String, MqttDeviceState> topicStateMap = new HashMap<>();
	@Autowired
	private ApiClient apiClient;

	private Set<String> subscribedTopics = new HashSet<>();

	public Set<Device> getDevices() {
		return devices;
	}

	public void publishStaticInfos(String pod, List<Device> devices) {
		try {
			mqttClient.publish("homie/" + pod + "/$name", m("Somfy MQTT Controller"));
			mqttClient.publish("homie/" + pod + "/$state", m("ready"));
			mqttClient.publish("homie/" + pod + "/$homie", m("3.0"));
			mqttClient.publish("homie/" + pod + "/$nodes",
					m(devices.stream().map(d -> d.getDeviceURL().substring(1 + d.getDeviceURL().lastIndexOf("/")))
							.collect(Collectors.joining(","))));
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	public void addDevice(MqttDevice device) {
		try {

			mqttClient.publish(toTopic(device) + "/$name", m(device.getUiClass()));
			mqttClient.publish(toTopic(device) + "/$properties", getProperties(device));
			for (Entry<String, MqttDeviceState> s : device.getStates().entrySet()) {

				MqttDeviceState state = s.getValue();
				String stateTopic = toTopic(device) + "/" + s.getKey();
				mqttClient.publish(stateTopic, m(state.getValue().toString()));
				mqttClient.publish(stateTopic + "/$name", m(s.getKey()));
				if (state.getName().equals("core:TargetClosureState")) {
					mqttClient.publish(stateTopic + "/$settable", m("true"));
					mqttClient.publish(stateTopic + "/$unit", m("%"));
					mqttClient.publish(stateTopic + "/$datatype", m("integer"));

				} else
					mqttClient.publish(stateTopic + "/$datatype", m(toType(state.getType())));

				topicStateMap.put(stateTopic, state);
				int qos=0;
				if (subscribedTopics.add(stateTopic + "/set")) {
					mqttClient.subscribe(new MqttSubscription[] {new MqttSubscription(stateTopic+"/set", qos)}, new IMqttMessageListener[] {this});
				}
			}
		} catch (MqttException e) {
			log.error("Error adding device",e);
		} 
	}

	private String toType(Integer type) {
		if (type == null)
			return null;
		return switch (type) {
		case 1 -> "integer";
		case 3 -> "string";
		case 6 -> "boolean";
		default -> "string";
		};
	}

	private MqttMessage getProperties(MqttDevice device) {

		return m(Strings.join(device.getStates().keySet(), ','));
	}

	private MqttMessage m(String value) {
		MqttMessage mqttMessage;
		if (value == null) {
			mqttMessage = new MqttMessage();
		}
		mqttMessage = new MqttMessage(value.getBytes(Charset.forName("utf8")));
		mqttMessage.setQos(0);
		return mqttMessage;
	}

	private String toTopic(MqttDevice device) {

		return String.format("homie/%s", device.toTopic());
	}

	public void reset() {
		this.devices.clear();
		topicStateMap.clear();

	}

	public void addDevice(Device d) {
		addDevice(toMqttDevice(d));
		this.devices.add(d);
	}

	private MqttDevice toMqttDevice(Device d) {

		return MqttDevice.builder().uri(d.getDeviceURL()).label(d.getLabel()).uiClass(d.getDefinition().getUiClass())
				.widgetName(d.getDefinition().getWidgetName())
				.states(d.getStates().stream().collect(Collectors.toMap(s -> s.getName(), s -> toState(d, s)))).build();
	}

	private MqttDeviceState toState(Device d, State s) {
		return MqttDeviceState.builder().name(s.getName()).type(s.getType()).value(s.getValue()).device(d).build();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message)  {

		try {
			System.out.println("Got message for topic " + topic);
			MqttDeviceState state = topicStateMap.get(topic.replace("/set", ""));
			String value = new String(message.getPayload(), Charset.forName("utf8"));
			ExecutionApi executionApi = new ExecutionApi(apiClient);
			ExecApplyPostRequestActionsInnerCommandsInner command = generateCommand(state, value);
			
			ExecApplyPostRequestActionsInner actions = ExecApplyPostRequestActionsInner.builder()

					.deviceURL(state.getDevice().getDeviceURL()).commands(Arrays.asList(command)).build();
			ExecApplyPostRequest exec = ExecApplyPostRequest.builder().label("MQTT Action")
					.actions(Arrays.asList(actions)).build();
			ExecApplyPost200Response response = executionApi.execApplyPost(exec);

			System.out.println(response.toString());
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private ExecApplyPostRequestActionsInnerCommandsInner generateCommand(MqttDeviceState state, String value) {
 value=value.toLowerCase();		
		String commandName = "setPosition";
		boolean useParameters=true;
		if( "up".equals(value))
			value="0";
		else if ("down".equals(value))
			value="100";
		else if ("stop".equals(value)) {
			commandName="stop";
			useParameters=false;
		}
		
		var command = ExecApplyPostRequestActionsInnerCommandsInner
				.builder().name(commandName);
		if(useParameters)
				command=command.parameters(Arrays.asList(convertToType(state.getType(), value)));
		return command.build();
	}

	private Object convertToType(Integer type, String value) {
		switch (type) {
		case 3:
			return value;
		case 1:
			return Integer.valueOf(value);
		case 6:
			return Boolean.valueOf(value);
		default:
			return value;
		}
	}

	@Override
	public void onSuccess(IMqttToken asyncActionToken) {
		if(asyncActionToken.getTopics()!=null)
		for(String t : asyncActionToken.getTopics())
			try {
				messageArrived(t, asyncActionToken.getMessage());
			} catch (MqttException e) {
				log.error("Error processing MQTT message",e);
			}
		
	}

	@Override
	public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
		log.error("error while receiving message",exception);
	}

}
