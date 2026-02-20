package br.centroweg.libera_ai.domain.port;

import br.centroweg.libera_ai.domain.event.ExitAccessEvent;

public interface ExitEventProducer {

    void send(ExitAccessEvent event);

}
