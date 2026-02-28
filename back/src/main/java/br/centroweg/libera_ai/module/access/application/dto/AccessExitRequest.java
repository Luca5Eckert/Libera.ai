package br.centroweg.libera_ai.module.access.application.dto;

import jakarta.validation.constraints.NotNull;

public record AccessExitRequest(
        @NotNull Integer code
) {
}
