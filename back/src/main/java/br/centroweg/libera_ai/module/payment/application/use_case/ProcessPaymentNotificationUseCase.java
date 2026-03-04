package br.centroweg.libera_ai.module.payment.application.use_case;

import br.centroweg.libera_ai.module.payment.domain.exception.PaymentException;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentProvider;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public record ProcessPaymentNotificationUseCase(
        PaymentRepository paymentRepository,
        PaymentProvider paymentProvider
) {

    @Transactional
    public void execute(String mercadoPagoPaymentId) {
        String currentStatus = paymentProvider.fetchStatus(mercadoPagoPaymentId);
        String internalPaymentId = paymentProvider.getExternalReference(mercadoPagoPaymentId);

        var payment = paymentRepository.findById(internalPaymentId)
                .orElseThrow(() -> new PaymentException("Payment not found for internal ID: " + internalPaymentId));

        if (payment.processStatusUpdate(mercadoPagoPaymentId, currentStatus)) {
            paymentRepository.save(payment);
        }
    }
}