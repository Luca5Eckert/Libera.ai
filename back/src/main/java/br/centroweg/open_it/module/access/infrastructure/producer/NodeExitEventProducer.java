package br.centroweg.open_it.module.access.infrastructure.producer;

import br.centroweg.open_it.module.access.domain.event.ExitAccessEvent;
import br.centroweg.open_it.module.access.domain.port.ExitEventProducer;
import br.centroweg.open_it.module.access.infrastructure.exception.IoTIntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class NodeExitEventProducer implements ExitEventProducer {

    private final RestTemplate restTemplate;
    private final String nodeUrl;

    public NodeExitEventProducer(
            RestTemplate restTemplate,
            @Value("${node.url}") String nodeUrl
    ) {
        this.restTemplate = restTemplate;
        this.nodeUrl = nodeUrl;
    }

    @Override
    public void send(ExitAccessEvent event) {
        try {
            log.info("Dispatching release signal for code: {}", event.code());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ExitAccessEvent> requestEntity = new HttpEntity<>(event, headers);

            restTemplate.postForEntity(nodeUrl, requestEntity, Void.class);

            log.info("Release signal successfully delivered to Node-RED at: {}", nodeUrl);
        } catch (Exception e) {
            log.error("Failed to communicate with Node-RED at {}. Error: {}", nodeUrl, e.getMessage());
            throw new IoTIntegrationException("IoT Integration Error: Could not reach Node-RED at " + nodeUrl, e);
        }
    }

}