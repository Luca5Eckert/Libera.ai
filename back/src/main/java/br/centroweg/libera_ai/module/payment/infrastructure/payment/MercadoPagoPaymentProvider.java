package br.centroweg.libera_ai.module.payment.infrastructure.payment;

import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentProvider;
import br.centroweg.libera_ai.module.payment.infrastructure.exception.PaymentIntegrationException;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
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

/**
 * Mercado Pago payment provider implementation using Checkout Pro.
 * <p>
 * Security notes:
 * - Access token is loaded from environment variables (never hardcoded)
 * - Uses MPRequestOptions to pass token per-request for better isolation
 * - All API calls are logged for debugging and audit
 */
@Service
public class MercadoPagoPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentProvider.class);

    private final PaymentClient paymentClient;
    private final String accessToken;
    private final String notificationUrl;

    public MercadoPagoPaymentProvider(
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.notification-url:}") String notificationUrl
    ) {

        log.info("[MP-PROVIDER] Initializing Mercado Pago Payment Provider (Checkout Pro)");

        MercadoPagoConfig.setAccessToken(accessToken);
        this.accessToken = accessToken;
        this.paymentClient = new PaymentClient();
        this.notificationUrl = notificationUrl;

    }

    public PaymentInfo generatePayment(double amount, String internalPaymentId) {
        try {
            PreferenceClient client = new PreferenceClient();

            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .id(internalPaymentId)
                    .title("Libera AI - Payment for Access " + internalPaymentId)
                    .quantity(1)
                    .unitPrice(new BigDecimal(String.valueOf(amount)))
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://overstudious-lani-patterny.ngrok-free.dev/success")
                    .failure("https://overstudious-lani-patterny.ngrok-free.dev/failure")
                    .pending("https://overstudious-lani-patterny.ngrok-free.dev/pending")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .backUrls(backUrls)
                    .externalReference(internalPaymentId)
                    .autoReturn("approved")
                    .notificationUrl(notificationUrl)
                    .build();

            Preference preference = client.create(preferenceRequest);

            return new PaymentInfo(
                    preference.getId(),
                    preference.getSandboxInitPoint(),
                    amount
            );

        } catch (MPException | MPApiException e) {
            throw new RuntimeException("Erro ao gerar preferência de pagamento: " + e.getMessage());
        }
    }

    @Override
    public String fetchStatus(String mercadoPagoPaymentId) {
        log.info("[MP-PROVIDER] Fetching payment status - MP Payment ID: {}", mercadoPagoPaymentId);

        try {
            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            Payment payment = paymentClient.get(Long.valueOf(mercadoPagoPaymentId), requestOptions);
            String status = payment.getStatus();

            log.info("[MP-PROVIDER] Payment status retrieved - MP Payment ID: {}, Status: {}, Status Detail: {}",
                    mercadoPagoPaymentId, status, payment.getStatusDetail());

            return status;
        } catch (NumberFormatException e) {
            log.error("[MP-PROVIDER] Invalid payment ID format: {}", mercadoPagoPaymentId);
            return "unknown";
        } catch (MPApiException e) {
            log.error("[MP-PROVIDER] Mercado Pago API error fetching status - MP Payment ID: {}, Status: {}, Content: {}",
                    mercadoPagoPaymentId, e.getStatusCode(), getApiResponseContent(e));
            return "pending";
        } catch (MPException e) {
            log.error("[MP-PROVIDER] Mercado Pago SDK error fetching status for {}: {}", mercadoPagoPaymentId, e.getMessage());
            return "pending";
        } catch (Exception e) {
            log.error("[MP-PROVIDER] Unexpected error fetching status for {}: {}", mercadoPagoPaymentId, e.getMessage(), e);
            return "pending";
        }
    }

    @Override
    public String getExternalReference(String mercadoPagoPaymentId) {
        log.info("[MP-PROVIDER] Fetching external reference - MP Payment ID: {}", mercadoPagoPaymentId);

        try {
            if (mercadoPagoPaymentId == null || mercadoPagoPaymentId.isBlank()) {
                log.warn("[MP-PROVIDER] Invalid payment ID provided for external reference fetch");
                return null;
            }

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            Payment payment = paymentClient.get(Long.valueOf(mercadoPagoPaymentId), requestOptions);
            String externalRef = payment.getExternalReference();

            log.info("[MP-PROVIDER] External reference retrieved - MP Payment ID: {}, External Reference: {}",
                    mercadoPagoPaymentId, externalRef);

            return externalRef;
        } catch (NumberFormatException e) {
            log.error("[MP-PROVIDER] Invalid payment ID format: {}", mercadoPagoPaymentId);
            return null;
        } catch (MPApiException e) {
            log.error("[MP-PROVIDER] Mercado Pago API error fetching external reference - MP Payment ID: {}, Status: {}, Content: {}",
                    mercadoPagoPaymentId, e.getStatusCode(), getApiResponseContent(e));
            return null;
        } catch (MPException e) {
            log.error("[MP-PROVIDER] Mercado Pago SDK error fetching external reference for {}: {}", mercadoPagoPaymentId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[MP-PROVIDER] Unexpected error fetching external reference for {}: {}", mercadoPagoPaymentId, e.getMessage(), e);
            return null;
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