package br.centroweg.libera_ai.infrastructure.producer;

import br.centroweg.libera_ai.module.access.domain.event.ExitAccessEvent;
import br.centroweg.libera_ai.module.access.domain.port.ExitEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

            restTemplate.postForEntity(nodeUrl, event, Void.class);

            log.info("Release signal successfully delivered to Node.js at: {}", nodeUrl);
        } catch (Exception e) {
            log.error("Failed to communicate with Node orchestrator at {}. Error: {}", nodeUrl, e.getMessage());
            throw new RuntimeException("IoT Integration Error: Could not reach the Node.js server", e);
        }
    }

}