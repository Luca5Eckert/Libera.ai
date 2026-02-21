package br.centroweg.libera_ai.infrastructure.exception;

public record ApiErrorResponse(
        int status,
        String message,
        String details
) {

    public static ApiErrorResponse of(int status, String message, String details) {
        return new ApiErrorResponse(status, message, details);
    }
}
