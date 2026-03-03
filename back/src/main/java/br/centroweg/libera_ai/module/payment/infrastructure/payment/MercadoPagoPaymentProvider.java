package br.centroweg.libera_ai.module.payment.infrastructure.payment;

import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentProvider;
import br.centroweg.libera_ai.module.payment.infrastructure.exception.PaymentIntegrationException;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;

@Service
public class MercadoPagoPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentProvider.class);

    private final PaymentClient paymentClient;
    private final PreferenceClient preferenceClient;
    private final String accessToken;
    private final String notificationUrl;

    public MercadoPagoPaymentProvider(
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.notification-url:}") String notificationUrl) {

        log.info("Iniciando MercadoPagoPaymentProvider com Checkout Pro...");

        MercadoPagoConfig.setAccessToken(accessToken);
        this.accessToken = accessToken;
        this.paymentClient = new PaymentClient();
        this.preferenceClient = new PreferenceClient();
        this.notificationUrl = notificationUrl;
    }

    @Override
    public PaymentInfo generatePayment(double amount) {
        try {
            log.info("Gerando preferência de pagamento (Checkout Pro) no valor de: {}", amount);

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Estacionamento Libera.ai")
                    .description("Pagamento de estadia - Libera.ai")
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(amount))
                    .build();

            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(Collections.singletonList(item))
                    .notificationUrl(notificationUrl.isEmpty() ? null : notificationUrl)
                    .backUrls(PreferenceBackUrlsRequest.builder()
                            .success("https://seu-app.com/success")
                            .failure("https://seu-app.com/failure")
                            .build())
                    .autoReturn("approved");

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            Preference preference = preferenceClient.create(builder.build(), requestOptions);

            log.info("Preferência criada com sucesso! ID: {}", preference.getId());

            return new PaymentInfo(
                    preference.getId(),
                    preference.getInitPoint(),
                    amount
            );

        } catch (MPApiException e) {
            log.error("Erro na API do Mercado Pago (Preference): {}", getApiResponseContent(e));
            throw new PaymentIntegrationException(buildMercadoPagoErrorMessage("generate preference", e), e);
        } catch (MPException e) {
            log.error("Erro inesperado no SDK: {}", e.getMessage());
            throw new PaymentIntegrationException("Failed to create preference: " + e.getMessage(), e);
        }
    }

    @Override
    public String fetchStatus(String externalId) {
        try {
            log.info("Buscando status do pagamento: {}", externalId);

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            Payment payment = paymentClient.get(Long.valueOf(externalId), requestOptions);
            return payment.getStatus();
        } catch (Exception e) {
            log.error("Erro ao buscar status: {}", e.getMessage());
            return "pending";
        }
    }

    private String buildMercadoPagoErrorMessage(String operation, MPApiException e) {
        return String.format("Mercado Pago API error during %s (HTTP %d): %s",
                operation, e.getStatusCode(), getApiResponseContent(e));
    }

    private String getApiResponseContent(MPApiException e) {
        return (e.getApiResponse() != null) ? e.getApiResponse().getContent() : "No response content";
    }
}