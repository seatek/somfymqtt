package net.seatek.home.somfy.somfymqtt;

import com.somfy.tahoma.model.Device;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MqttDeviceState {
private String name;
private Object value;
private Integer type;
private Device device;
}
