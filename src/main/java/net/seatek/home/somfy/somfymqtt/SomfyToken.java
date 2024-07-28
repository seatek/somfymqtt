package net.seatek.home.somfy.somfymqtt;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SomfyToken {
	private String token;
	private LocalDate date;
}
