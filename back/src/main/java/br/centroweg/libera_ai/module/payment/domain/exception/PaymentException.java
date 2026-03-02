package br.centroweg.libera_ai.module.payment.domain.exception;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
