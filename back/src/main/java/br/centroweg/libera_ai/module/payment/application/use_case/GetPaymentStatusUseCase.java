package br.centroweg.libera_ai.module.payment.application.use_case;

import br.centroweg.libera_ai.module.payment.domain.model.Payment;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public record GetPaymentStatusUseCase(PaymentRepository paymentRepository) {

    @Transactional(readOnly = true)
    public boolean execute(String externalPaymentId) {
        return paymentRepository.findByExternalId(externalPaymentId)
                .map(Payment::isPaid)
                .orElse(false);
    }
}