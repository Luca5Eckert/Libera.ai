package br.centroweg.libera_ai.module.access.domain.exception;

public class AccessCodeNotValidException extends AccessDomainException {
    public AccessCodeNotValidException(String message) {
        super(message);
    }

    public AccessCodeNotValidException() {
        super("Access code is not valid.");
    }
}
