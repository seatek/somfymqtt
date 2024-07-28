package net.seatek.home.somfy.somfymqtt.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateProducer {
	@Bean
	public RestTemplate resttemplate() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
	       
		
		TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
		SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
		final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
				NoopHostnameVerifier.INSTANCE);
		final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
				.<ConnectionSocketFactory>create().register("https", sslsf)
				.register("http", new PlainConnectionSocketFactory()).build();
		final HttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
				socketFactoryRegistry);
		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).
				build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		return restTemplate;
	}
}
