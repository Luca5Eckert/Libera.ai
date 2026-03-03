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
    private final String defaultEmail;

    public MercadoPagoPaymentProvider(
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.default-payer-email}") String defaultEmail) {

        log.info("Iniciando MercadoPagoPaymentProvider...");
        log.info("Token injetado: {}", accessToken);
        log.info("E-mail do pagador injetado: {}", defaultEmail);

        MercadoPagoConfig.setAccessToken(accessToken);
        this.paymentClient = new PaymentClient();
        this.defaultEmail = defaultEmail;
    }

    @Override
    public PaymentInfo generatePayment(double amount) {
        try {
            // Log antes da chamada
            log.info("Gerando pagamento PIX...");
            log.debug("Token ativo no MercadoPagoConfig: {}", MercadoPagoConfig.getAccessToken());
            log.info("Enviando requisição para e-mail: {}", defaultEmail);

            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .transactionAmount(BigDecimal.valueOf(amount))
                    .paymentMethodId("pix")
                    .payer(PaymentPayerRequest.builder()
                            .email(defaultEmail)
                            .build())
                    .build();

            Payment payment = paymentClient.create(request);

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
            Long mpId = Long.valueOf(externalId);
            Payment payment = paymentClient.get(mpId);
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
            return "Mercado Pago authentication error: You are using LIVE/production credentials in a test environment. " +
                    "Please use TEST credentials (token starting with 'TEST-') for development and testing.";
        }

        return String.format("Mercado Pago API error during %s (HTTP %d): %s", operation, statusCode, apiResponse);
    }

    private String getApiResponseContent(MPApiException e) {
        if (e.getApiResponse() == null) return "No response content";
        return e.getApiResponse().getContent();
    }
}