package br.centroweg.libera_ai.module.access.domain.port;

import br.centroweg.libera_ai.module.access.domain.event.ExitAccessEvent;

public interface ExitEventProducer {

    void send(ExitAccessEvent event);

}
