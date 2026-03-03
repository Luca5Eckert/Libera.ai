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
        String currentStatus = paymentProvider.fetchStatus(mercadoPagoPaymentId);

        String internalPaymentId = paymentProvider.getExternalReference(mercadoPagoPaymentId);

        var payment = paymentRepository.findById(internalPaymentId)
                .orElseThrow(() -> new PaymentException("Payment not found for internal ID: " + internalPaymentId));

        boolean wasUpdated = payment.processStatusUpdate(mercadoPagoPaymentId, currentStatus);

        if (!wasUpdated) {
            return;
        }

        paymentRepository.save(payment);
    }

}