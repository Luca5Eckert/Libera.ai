package br.centroweg.libera_ai.module.payment.presentation.mapper;

import br.centroweg.libera_ai.module.payment.presentation.dto.PaymentResponse;
import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(PaymentInfo paymentInfo) {
        return new PaymentResponse(
                paymentInfo.amount(),
                paymentInfo.linkPayment(),
                paymentInfo.generatedPaymentId()
        );
    }

}
