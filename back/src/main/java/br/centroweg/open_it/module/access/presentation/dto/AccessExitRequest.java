package br.centroweg.open_it.module.access.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AccessExitRequest(
        @NotNull Integer code
) {
}
