package br.centroweg.libera_ai.module.payment.infrastructure.exception;

public class PaymentIntegrationException extends RuntimeException {
    public PaymentIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

}
