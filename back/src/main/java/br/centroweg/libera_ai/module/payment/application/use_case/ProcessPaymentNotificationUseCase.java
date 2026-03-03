package br.centroweg.libera_ai.module.payment.application.use_case;

import br.centroweg.libera_ai.module.payment.domain.exception.PaymentException;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentProvider;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProcessPaymentNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentNotificationUseCase.class);

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;

    public ProcessPaymentNotificationUseCase(PaymentRepository paymentRepository, PaymentProvider paymentProvider) {
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
    }

    /**
     * Process a payment notification from Mercado Pago.
     * This method validates the notification by fetching the payment from MP API,
     * implements idempotency to avoid duplicate processing, and properly handles all payment statuses.
     *
     * @param mercadoPagoPaymentId The payment ID received from Mercado Pago webhook
     */
    @Transactional
    public void execute(String mercadoPagoPaymentId) {
        log.info("[WEBHOOK] Processing payment notification for MP Payment ID: {}", mercadoPagoPaymentId);

        if (mercadoPagoPaymentId == null || mercadoPagoPaymentId.isBlank()) {
            log.warn("[WEBHOOK] Received null or empty payment ID, ignoring notification");
            return;
        }

        try {
            // Step 1: Validate notification by fetching payment from Mercado Pago API
            // This prevents bypass attacks where someone sends fake notifications
            String currentStatus = paymentProvider.fetchStatus(mercadoPagoPaymentId);
            log.info("[WEBHOOK] Payment status from Mercado Pago API: {} for MP Payment ID: {}", currentStatus, mercadoPagoPaymentId);

            // Step 2: Get the external_reference which is our internal payment ID
            String internalPaymentId = paymentProvider.getExternalReference(mercadoPagoPaymentId);
            if (internalPaymentId == null || internalPaymentId.isEmpty()) {
                log.warn("[WEBHOOK] No external_reference found for MP Payment ID: {}. This may be a payment not related to our system.", mercadoPagoPaymentId);
                return; // Don't throw exception, just ignore - this could be a test payment
            }

            log.info("[WEBHOOK] Found internal payment ID: {} for MP Payment ID: {}", internalPaymentId, mercadoPagoPaymentId);

            // Step 3: Find our payment by internal ID
            var paymentOpt = paymentRepository.findById(internalPaymentId);
            if (paymentOpt.isEmpty()) {
                log.error("[WEBHOOK] Payment not found for internal ID: {}. This should not happen.", internalPaymentId);
                throw new PaymentException("Payment not found for internal ID: " + internalPaymentId);
            }

            var payment = paymentOpt.get();

            // Step 4: Process status update with idempotency check
            boolean wasUpdated = payment.processStatusUpdate(mercadoPagoPaymentId, currentStatus);
            
            if (!wasUpdated) {
                log.info("[WEBHOOK] Duplicate notification ignored - MP Payment ID: {} already processed with status: {}", 
                        mercadoPagoPaymentId, currentStatus);
                return;
            }

            // Step 5: Save the updated payment
            paymentRepository.save(payment);
            
            // Step 6: Log result based on status
            switch (currentStatus.toLowerCase()) {
                case "approved" -> log.info("[WEBHOOK] ✓ Payment APPROVED - Internal ID: {}, MP Payment ID: {}", 
                        internalPaymentId, mercadoPagoPaymentId);
                case "pending", "in_process", "authorized" -> log.info("[WEBHOOK] ⏳ Payment PENDING - Internal ID: {}, MP Payment ID: {}, Status: {}", 
                        internalPaymentId, mercadoPagoPaymentId, currentStatus);
                case "rejected", "cancelled", "refunded", "charged_back" -> log.warn("[WEBHOOK] ✗ Payment REJECTED/CANCELLED - Internal ID: {}, MP Payment ID: {}, Status: {}", 
                        internalPaymentId, mercadoPagoPaymentId, currentStatus);
                default -> log.info("[WEBHOOK] Payment status updated - Internal ID: {}, MP Payment ID: {}, Status: {}", 
                        internalPaymentId, mercadoPagoPaymentId, currentStatus);
            }

        } catch (Exception e) {
            // Log all errors but don't re-throw - webhook handler should always return 200
            // to prevent Mercado Pago from sending endless retries for errors we can't fix
            // (like invalid external_reference, payment not found, etc.)
            log.error("[WEBHOOK] Error processing payment notification for MP Payment ID {}: {} ({})", 
                    mercadoPagoPaymentId, e.getMessage(), e.getClass().getSimpleName());
        }
    }

}