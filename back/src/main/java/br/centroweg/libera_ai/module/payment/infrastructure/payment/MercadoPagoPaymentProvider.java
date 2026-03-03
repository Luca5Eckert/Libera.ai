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

/**
 * Mercado Pago payment provider implementation using direct PIX payment.
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
    private final String accessToken;
    private final String notificationUrl;
    private final String defaultPayerEmail;

    public MercadoPagoPaymentProvider(
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.notification-url:}") String notificationUrl,
            @Value("${mercadopago.default-payer-email:}") String defaultPayerEmail) {

        log.info("[MP-PROVIDER] Initializing Mercado Pago Payment Provider (PIX)");

        // Validate access token is present
        if (accessToken == null || accessToken.isBlank()) {
            log.error("[MP-PROVIDER] Access token is not configured! Check MP_ACCESS_TOKEN environment variable.");
            throw new IllegalStateException("Mercado Pago access token is not configured");
        }

        // Log token type (TEST vs LIVE) for debugging - never log the actual token
        if (accessToken.startsWith("TEST-")) {
            log.info("[MP-PROVIDER] Using TEST credentials (sandbox mode)");
            if (defaultPayerEmail == null || defaultPayerEmail.isBlank()) {
                log.warn("[MP-PROVIDER] TEST mode detected but no default payer email configured. " +
                        "Set MP_DEFAULT_PAYER_EMAIL to a test buyer account to avoid errors.");
            }
        } else if (accessToken.startsWith("APP_USR-")) {
            log.info("[MP-PROVIDER] Using LIVE credentials (production mode)");
        } else {
            log.warn("[MP-PROVIDER] Access token format not recognized. Ensure you're using valid MP credentials.");
        }

        MercadoPagoConfig.setAccessToken(accessToken);
        this.accessToken = accessToken;
        this.paymentClient = new PaymentClient();
        this.notificationUrl = notificationUrl;
        this.defaultPayerEmail = defaultPayerEmail;

        if (notificationUrl != null && !notificationUrl.isBlank()) {
            log.info("[MP-PROVIDER] Webhook URL configured: {}", notificationUrl);
        } else {
            log.warn("[MP-PROVIDER] No webhook URL configured. Payment notifications will not be received automatically.");
        }
    }

    @Override
    public PaymentInfo generatePayment(double amount, String internalPaymentId) {
        log.info("[MP-PROVIDER] Generating PIX payment - Amount: R$ {}, Internal ID: {}", amount, internalPaymentId);

        try {
            // Validate inputs
            if (amount <= 0) {
                throw new PaymentIntegrationException("Payment amount must be greater than zero");
            }
            if (internalPaymentId == null || internalPaymentId.isBlank()) {
                throw new PaymentIntegrationException("Internal payment ID is required");
            }

            String payerEmail = (defaultPayerEmail != null && !defaultPayerEmail.isBlank())
                    ? defaultPayerEmail
                    : "pagamento@liberaai.com";

            PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                    .transactionAmount(BigDecimal.valueOf(amount))
                    .description("Estacionamento Libera.ai")
                    .paymentMethodId("pix")
                    .externalReference(internalPaymentId)
                    .notificationUrl(notificationUrl != null && !notificationUrl.isEmpty() ? notificationUrl : null)
                    .payer(PaymentPayerRequest.builder()
                            .email(payerEmail)
                            .build())
                    .build();

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            Payment payment = paymentClient.create(createRequest, requestOptions);

            String qrCode = null;
            String qrCodeBase64 = null;
            String ticketUrl = null;

            if (payment.getPointOfInteraction() != null 
                    && payment.getPointOfInteraction().getTransactionData() != null) {
                qrCode = payment.getPointOfInteraction().getTransactionData().getQrCode();
                qrCodeBase64 = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();
                ticketUrl = payment.getPointOfInteraction().getTransactionData().getTicketUrl();
            }

            log.info("[MP-PROVIDER] PIX payment created successfully - MP Payment ID: {}, External Reference: {}", 
                    payment.getId(), internalPaymentId);

            return new PaymentInfo(
                    String.valueOf(payment.getId()),
                    ticketUrl,
                    amount,
                    qrCode,
                    qrCodeBase64,
                    ticketUrl
            );

        } catch (MPApiException e) {
            String errorContent = getApiResponseContent(e);
            log.error("[MP-PROVIDER] Mercado Pago API error creating PIX payment - Status: {}, Content: {}", 
                    e.getStatusCode(), errorContent);
            throw new PaymentIntegrationException(buildMercadoPagoErrorMessage("create PIX payment", e), e);
        } catch (MPException e) {
            log.error("[MP-PROVIDER] Mercado Pago SDK error: {}", e.getMessage(), e);
            throw new PaymentIntegrationException("Failed to create PIX payment: " + e.getMessage(), e);
        } catch (PaymentIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MP-PROVIDER] Unexpected error creating PIX payment: {}", e.getMessage(), e);
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