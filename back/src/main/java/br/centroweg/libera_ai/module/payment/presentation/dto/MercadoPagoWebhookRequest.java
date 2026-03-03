package br.centroweg.libera_ai.module.payment.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Mercado Pago webhook notifications.
 * 
 * Mercado Pago sends notifications in the following format:
 * {
 *   "id": 12345,
 *   "live_mode": true,
 *   "type": "payment",
 *   "date_created": "2015-03-25T10:04:58.396-04:00",
 *   "user_id": 44444,
 *   "api_version": "v1",
 *   "action": "payment.updated",
 *   "data": {
 *     "id": "999999999"
 *   }
 * }
 * 
 * @see <a href="https://www.mercadopago.com.br/developers/en/docs/your-integrations/notifications/webhooks">Mercado Pago Webhooks</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoWebhookRequest(
        Long id,
        @JsonProperty("live_mode") Boolean liveMode,
        String type,
        String action,
        @JsonProperty("api_version") String apiVersion,
        @JsonProperty("date_created") String dateCreated,
        @JsonProperty("user_id") Long userId,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("id") String paymentId
    ) {}
}