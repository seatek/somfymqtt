package net.seatek.home.somfy.somfymqtt;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClientException;

import com.somfy.tahoma.api.ApiApi;
import com.somfy.tahoma.api.EventApi;
import com.somfy.tahoma.api.GatewayApi;
import com.somfy.tahoma.api.SetupApi;
import com.somfy.tahoma.invoker.ApiClient;
import com.somfy.tahoma.model.ApiVersionGet200Response;
import com.somfy.tahoma.model.Device;
import com.somfy.tahoma.model.Event;
import com.somfy.tahoma.model.Event.NameEnum;
import com.somfy.tahoma.model.EventsRegisterPost200Response;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class SomfyMqttApplication {
	@Autowired
	private MqttRegistry deviceRegistry;

	@Autowired
	private ApiClient apiClient;
	private UUID listenerId;
	private EventApi eventApi;
	private SetupApi setupApi;
	private ApiApi apiInstance;

	public static void main(String[] args) {
		SpringApplication.run(SomfyMqttApplication.class, args);
	}

	@PostConstruct
	public void init() {
		apiInstance = new ApiApi(apiClient);

		// System.out.println(result);

		new GatewayApi(apiClient);
		setupApi = new SetupApi(apiClient);
		eventApi = new EventApi(apiClient);

		start();
	}

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
	public void scheduler() {
		// start();

		try {
			List<Event> events = eventApi.eventsListenerIdFetchPost(listenerId);
			for (Event e : events) {
				
				NameEnum name = e.getName();
				if(name==NameEnum.DEVICESTATECHANGEDEVENT)
					start();
				
			}
		} catch (RestClientException e) {
			if(e.getMessage().contains("No registered event listener"))
				registerEventListener();
			else
			e.printStackTrace();
		}
	}

	@Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
	public void schedulerFullPublish() {
		start();		
	}
	
	
	public void start() {

		ApiVersionGet200Response result = apiInstance.apiVersionGet();
		log.debug(result.toString());
		try (FileWriter deviceFile = new FileWriter("devices.json")) {

			List<Device> devices = setupApi.setupDevicesGet();
			// deviceRegistry.reset();
			deviceRegistry.publishStaticInfos("0818-1361-3971", devices);
			for (Device d : devices) {

				deviceFile.write(d.toString());

				deviceRegistry.addDevice(d);
			}
			registerEventListener();

		} catch (IOException e) {
			log.error("Could not start application",e);
		}

	}

	private void registerEventListener() {
		EventsRegisterPost200Response eventResponse = eventApi.eventsRegisterPost();
		listenerId = eventResponse.getId();
	}

}
