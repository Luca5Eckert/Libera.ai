package br.centroweg.libera_ai.module.payment.infrastructure.validator;

import br.centroweg.libera_ai.module.access.domain.port.PaymentValidator;
import br.centroweg.libera_ai.module.payment.domain.model.Payment;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentValidatorAdapter implements PaymentValidator {

    private final PaymentRepository paymentRepository;

    public PaymentValidatorAdapter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public boolean isPaymentValid(int code) {
        Optional<Payment> paymentOptional = paymentRepository.findByAccessCode(code);

        return paymentOptional
                .map(Payment::isReadyForExit)
                .orElse(false);
    }

}
