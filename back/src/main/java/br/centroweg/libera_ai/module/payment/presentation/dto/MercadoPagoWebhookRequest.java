package br.centroweg.libera_ai.module.payment.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MercadoPagoWebhookRequest(
        String action,
        @JsonProperty("api_version") String apiVersion,
        Data data
) {
    public record Data(
            @JsonProperty("id") String paymentId
    ) {}
}