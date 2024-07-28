package net.seatek.home.somfy.somfymqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Token {
	private String token;
	String label;
	String scope;
}
