package br.centroweg.libera_ai.module.payment.domain.port;

import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;

public interface PaymentProvider {

    PaymentInfo generatePayment(double mount);

    String fetchStatus(String externalId);

}
