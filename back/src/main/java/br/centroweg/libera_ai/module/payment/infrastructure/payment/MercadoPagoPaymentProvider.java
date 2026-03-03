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

/**
 * Mercado Pago payment provider implementation using Checkout Pro.
 * 
 * Security notes:
 * - Access token is loaded from environment variables (never hardcoded)
 * - Uses MPRequestOptions to pass token per-request for better isolation
 * - All API calls are logged for debugging and audit
 */
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

        log.info("[MP-PROVIDER] Initializing Mercado Pago Payment Provider (Checkout Pro)");

        // Validate access token is present
        if (accessToken == null || accessToken.isBlank()) {
            log.error("[MP-PROVIDER] Access token is not configured! Check MP_ACCESS_TOKEN environment variable.");
            throw new IllegalStateException("Mercado Pago access token is not configured");
        }

        // Log token type (TEST vs LIVE) for debugging - never log the actual token
        if (accessToken.startsWith("TEST-")) {
            log.info("[MP-PROVIDER] Using TEST credentials (sandbox mode)");
        } else if (accessToken.startsWith("APP_USR-")) {
            log.info("[MP-PROVIDER] Using LIVE credentials (production mode)");
        } else {
            log.warn("[MP-PROVIDER] Access token format not recognized. Ensure you're using valid MP credentials.");
        }

        MercadoPagoConfig.setAccessToken(accessToken);
        this.accessToken = accessToken;
        this.paymentClient = new PaymentClient();
        this.preferenceClient = new PreferenceClient();
        this.notificationUrl = notificationUrl;

        if (notificationUrl != null && !notificationUrl.isBlank()) {
            log.info("[MP-PROVIDER] Webhook URL configured: {}", notificationUrl);
        } else {
            log.warn("[MP-PROVIDER] No webhook URL configured. Payment notifications will not be received automatically.");
        }
    }

    @Override
    public PaymentInfo generatePayment(double amount, String internalPaymentId) {
        log.info("[MP-PROVIDER] Generating payment preference - Amount: R$ {}, Internal ID: {}", amount, internalPaymentId);

        try {
            // Validate inputs
            if (amount <= 0) {
                throw new PaymentIntegrationException("Payment amount must be greater than zero");
            }
            if (internalPaymentId == null || internalPaymentId.isBlank()) {
                throw new PaymentIntegrationException("Internal payment ID is required");
            }

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Estacionamento Libera.ai")
                    .description("Pagamento de estadia - Libera.ai")
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(amount))
                    .currencyId("BRL")
                    .build();

            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(Collections.singletonList(item))
                    .externalReference(internalPaymentId)
                    .notificationUrl(notificationUrl.isEmpty() ? null : notificationUrl)
                    .backUrls(PreferenceBackUrlsRequest.builder()
                            .success("https://seu-app.com/success")
                            .failure("https://seu-app.com/failure")
                            .pending("https://seu-app.com/pending")
                            .build())
                    .autoReturn("approved");

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            Preference preference = preferenceClient.create(builder.build(), requestOptions);

            log.info("[MP-PROVIDER] Preference created successfully - Preference ID: {}, External Reference: {}, Init Point: {}", 
                    preference.getId(), internalPaymentId, preference.getInitPoint());

            return new PaymentInfo(
                    preference.getId(),
                    preference.getInitPoint(),
                    amount
            );

        } catch (MPApiException e) {
            String errorContent = getApiResponseContent(e);
            log.error("[MP-PROVIDER] Mercado Pago API error creating preference - Status: {}, Content: {}", 
                    e.getStatusCode(), errorContent);
            throw new PaymentIntegrationException(buildMercadoPagoErrorMessage("create preference", e), e);
        } catch (MPException e) {
            log.error("[MP-PROVIDER] Mercado Pago SDK error: {}", e.getMessage(), e);
            throw new PaymentIntegrationException("Failed to create payment preference: " + e.getMessage(), e);
        } catch (PaymentIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MP-PROVIDER] Unexpected error creating preference: {}", e.getMessage(), e);
            throw new PaymentIntegrationException("Unexpected error creating payment: " + e.getMessage(), e);
        }
    }

    @Override
    public String fetchStatus(String mercadoPagoPaymentId) {
        log.info("[MP-PROVIDER] Fetching payment status - MP Payment ID: {}", mercadoPagoPaymentId);

        try {
            if (mercadoPagoPaymentId == null || mercadoPagoPaymentId.isBlank()) {
                log.warn("[MP-PROVIDER] Invalid payment ID provided for status fetch");
                return "unknown";
            }

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
            return "pending"; // Return pending to avoid false negatives
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