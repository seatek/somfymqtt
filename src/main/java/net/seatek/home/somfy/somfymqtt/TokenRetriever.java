package net.seatek.home.somfy.somfymqtt;

import java.net.HttpCookie;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class TokenRetriever {
	@Value("${somfy.userName}")
	private String userId;

	@Value("${somfy.userPassword}")
	private String userPassword;
	
	@Value("${somfy.pod}")
	private String pod;

	public SomfyToken getToken() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("userId", userId);
		form.add("userPassword", userPassword);

		HttpEntity<MultiValueMap<String, String>> formRequest = new HttpEntity<MultiValueMap<String, String>>(form,
				headers);

		HttpCookie httpCookie = getSessionCookie(formRequest);

		Token token = getToken(httpCookie);

		activateToken(httpCookie, token);

		return new SomfyToken(token.getToken(), LocalDate.now());
	}

	private HttpCookie getSessionCookie(HttpEntity<MultiValueMap<String, String>> formRequest) {
		RestTemplate template = new RestTemplate();
		
		ResponseEntity<LoginResponse> result = template.postForEntity(
				"https://ha101-1.overkiz.com/enduser-mobile-web/enduserAPI/login", formRequest, LoginResponse.class);
		List<String> cookies = result.getHeaders().get("Set-Cookie");

		String jsession = cookies.get(0);
		HttpCookie httpCookie = HttpCookie.parse(jsession).get(0);
		return httpCookie;
	}

	private Token getToken(HttpCookie httpCookie) {
		RestTemplate template = new RestTemplate();
		RequestEntity<Void> getTokenRequest = RequestEntity
				.get("https://ha101-1.overkiz.com/enduser-mobile-web/enduserAPI/config/{pod}/local/tokens/generate",
						pod)
				.header("Cookie", httpCookie.getName() + "=" + httpCookie.getValue()).build();
		ResponseEntity<Token> getTokenResponse = template.exchange(getTokenRequest, Token.class);
		Token token = getTokenResponse.getBody();
		return token;
	}

	private Token activateToken(HttpCookie httpCookie, Token token) {
		Token tokenToActivate = token.toBuilder().scope("devmode").label("mqtt").build();
		RestTemplate template = new RestTemplate();
		RequestEntity<Token> getTokenRequest = RequestEntity
				.post("https://ha101-1.overkiz.com/enduser-mobile-web/enduserAPI/config/{pod}/local/tokens", pod)
				.header("Cookie", httpCookie.getName() + "=" + httpCookie.getValue()).body(tokenToActivate);

		ResponseEntity<Token> getTokenResponse = template.exchange(getTokenRequest, Token.class);
		Token tokenResponse = getTokenResponse.getBody();
		return tokenResponse;
	}
}
