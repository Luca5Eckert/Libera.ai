package br.centroweg.open_it.module.access.infrastructure.exception;

public class IoTIntegrationException extends RuntimeException {
    public IoTIntegrationException(String message) {
        super(message);
    }

    public IoTIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
