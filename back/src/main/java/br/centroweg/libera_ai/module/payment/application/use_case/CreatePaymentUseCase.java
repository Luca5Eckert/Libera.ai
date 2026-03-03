package br.centroweg.libera_ai.module.payment.application.use_case;

import br.centroweg.libera_ai.module.access.domain.exception.AccessDomainException;
import br.centroweg.libera_ai.module.access.domain.model.Access;
import br.centroweg.libera_ai.module.access.domain.port.AccessRepository;
import br.centroweg.libera_ai.module.payment.domain.model.Payment;
import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentProvider;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreatePaymentUseCase {

    private final PaymentProvider paymentProvider;
    private final PaymentRepository paymentRepository;
    private final AccessRepository accessRepository;

    public CreatePaymentUseCase(PaymentProvider paymentProvider, PaymentRepository paymentRepository, AccessRepository accessRepository) {
        this.paymentProvider = paymentProvider;
        this.paymentRepository = paymentRepository;
        this.accessRepository = accessRepository;
    }

    @Transactional
    public PaymentInfo execute(int code) {
        Access access = accessRepository.findByCodeAndExitIsNull(code)
                .orElseThrow(() -> new AccessDomainException("Access code not valid"));

        Payment payment = Payment.of(access);

        PaymentInfo info = paymentProvider.generatePayment(payment.getAmount(), payment.getId());

        payment.setExternalId(info.generatedPaymentId());

        paymentRepository.save(payment);

        return info;
    }

}