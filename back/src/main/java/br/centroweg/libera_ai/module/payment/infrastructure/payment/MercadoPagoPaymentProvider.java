package br.centroweg.libera_ai.module.payment.infrastructure.payment;

import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentProvider;
import br.centroweg.libera_ai.module.payment.infrastructure.exception.PaymentIntegrationException;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.payment.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MercadoPagoPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentProvider.class);
    private final PaymentClient paymentClient;
    private final String accessToken;
    private final String defaultEmail;
    private final String notificationUrl;

    public MercadoPagoPaymentProvider(
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.default-payer-email}") String defaultEmail,
            @Value("${mercadopago.notification-url:}") String notificationUrl) {

        log.info("Iniciando MercadoPagoPaymentProvider...");
        log.info("Token injetado: {}", accessToken);
        log.info("E-mail do pagador injetado: {}", defaultEmail);
        log.info("Notification URL: {}", notificationUrl.isEmpty() ? "(não configurada)" : notificationUrl);

        MercadoPagoConfig.setAccessToken(accessToken);
        this.accessToken = accessToken;
        this.paymentClient = new PaymentClient();
        this.defaultEmail = defaultEmail;
        this.notificationUrl = notificationUrl;
    }

    @Override
    public PaymentInfo generatePayment(double amount) {
        try {
            log.info("Gerando pagamento PIX...");
            log.info("Enviando requisição para e-mail: {}", defaultEmail);

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            PaymentCreateRequest.PaymentCreateRequestBuilder builder = PaymentCreateRequest.builder()
                    .transactionAmount(BigDecimal.valueOf(amount))
                    .description("Estacionamento Libera.ai")
                    .paymentMethodId("pix")
                    .payer(PaymentPayerRequest.builder()
                            .email(defaultEmail)
                            .build());

            if (!notificationUrl.isEmpty()) {
                builder.notificationUrl(notificationUrl);
            }

            Payment payment = paymentClient.create(builder.build(), requestOptions);

            log.info("Pagamento criado com sucesso! ID MP: {}", payment.getId());

            String generatedPaymentId = String.valueOf(payment.getId());
            String qrCode = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();

            return new PaymentInfo(generatedPaymentId, qrCode, amount);

        } catch (MPApiException e) {
            log.error("Erro na API do Mercado Pago: {}", getApiResponseContent(e));
            throw new PaymentIntegrationException(buildMercadoPagoErrorMessage("generate payment", e), e);
        } catch (MPException e) {
            log.error("Erro inesperado no SDK do Mercado Pago: {}", e.getMessage());
            throw new PaymentIntegrationException("Failed to create payment with Mercado Pago: " + e.getMessage(), e);
        }
    }

    @Override
    public String fetchStatus(String externalId) {
        try {
            log.info("Buscando status do pagamento: {}", externalId);

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            Long mpId = Long.valueOf(externalId);
            Payment payment = paymentClient.get(mpId, requestOptions);
            log.info("Status retornado: {}", payment.getStatus());
            return payment.getStatus();
        } catch (MPApiException e) {
            throw new PaymentIntegrationException(buildMercadoPagoErrorMessage("fetch payment status", e), e);
        } catch (MPException e) {
            throw new PaymentIntegrationException("Failed to fetch payment status with Mercado Pago: " + e.getMessage(), e);
        }
    }

    private String buildMercadoPagoErrorMessage(String operation, MPApiException e) {
        String apiResponse = getApiResponseContent(e);
        int statusCode = e.getStatusCode();

        if (statusCode == 401 && apiResponse.contains("Unauthorized use of live credentials")) {
            return "Mercado Pago authentication error: Ensure the MP_ACCESS_TOKEN is a TEST token (starting with 'TEST-') " +
                    "and MP_DEFAULT_PAYER_EMAIL is a test user created under the same Mercado Pago developer account. " +
                    "Visit https://www.mercadopago.com.br/developers/panel/app to create matching test users.";
        }

        return String.format("Mercado Pago API error during %s (HTTP %d): %s", operation, statusCode, apiResponse);
    }

    private String getApiResponseContent(MPApiException e) {
        if (e.getApiResponse() == null) return "No response content";
        return e.getApiResponse().getContent();
    }
}