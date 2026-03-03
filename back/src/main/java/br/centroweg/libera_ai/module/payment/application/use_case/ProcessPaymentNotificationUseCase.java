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

    @Transactional
    public void execute(String mercadoPagoPaymentId) {
        log.info("Processing payment notification for Mercado Pago payment ID: {}", mercadoPagoPaymentId);

        // Fetch status from Mercado Pago
        String currentStatus = paymentProvider.fetchStatus(mercadoPagoPaymentId);
        log.info("Payment status from Mercado Pago: {}", currentStatus);

        // Get the external_reference which is our internal payment ID
        String internalPaymentId = paymentProvider.getExternalReference(mercadoPagoPaymentId);
        if (internalPaymentId == null || internalPaymentId.isEmpty()) {
            log.warn("No external_reference found for Mercado Pago payment ID: {}", mercadoPagoPaymentId);
            throw new PaymentException("Payment not found - no external reference");
        }

        log.info("Found internal payment ID: {}", internalPaymentId);

        // Find our payment by internal ID
        var payment = paymentRepository.findById(internalPaymentId)
                .orElseThrow(() -> new PaymentException("Payment not found for internal ID: " + internalPaymentId));

        payment.confirmPayment(currentStatus);

        paymentRepository.save(payment);
        log.info("Payment {} updated with status: {}", internalPaymentId, currentStatus);
    }

}