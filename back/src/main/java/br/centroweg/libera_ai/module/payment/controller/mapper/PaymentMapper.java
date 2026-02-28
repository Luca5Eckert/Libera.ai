package br.centroweg.libera_ai.module.payment.controller.mapper;

import br.centroweg.libera_ai.module.payment.controller.dto.PaymentResponse;
import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(PaymentInfo paymentInfo) {
        return new PaymentResponse(
                paymentInfo.amount(),
                paymentInfo.qrCode(),
                paymentInfo.generatedPaymentId()
        );
    }

}
