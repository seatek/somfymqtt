package net.seatek.home.somfy.somfymqtt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.somfy.tahoma.invoker.ApiClient;

import net.seatek.home.somfy.somfymqtt.TokenRegistry;

@Component
public class SomfyApiProvider {
	@Value("${device.url}")
	private String deviceUrl;
	@Autowired
	private TokenRegistry tokenRegistry;
	@Bean
	public ApiClient provideApiClient(RestTemplate restTemplate) {
		ApiClient defaultClient = new ApiClient(restTemplate);
		defaultClient.setBearerToken(tokenRegistry.provideToken().getToken());
		defaultClient.setBasePath(deviceUrl + "/enduser-mobile-web/1/enduserAPI");
		return defaultClient;
	}
}
