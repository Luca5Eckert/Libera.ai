package br.centroweg.libera_ai.module.payment.controller.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull Integer accessCode
) {
}
