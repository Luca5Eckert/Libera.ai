package br.centroweg.libera_ai.module.access.domain.event;

public record ExitAccessEvent(
        int code
) {
    public static ExitAccessEvent of(int code) {
        return new ExitAccessEvent(code);
    }
}
