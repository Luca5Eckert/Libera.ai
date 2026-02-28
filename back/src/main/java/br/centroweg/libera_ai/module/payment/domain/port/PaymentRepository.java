package br.centroweg.libera_ai.module.payment.domain.port;

import br.centroweg.libera_ai.module.payment.domain.model.Payment;

public interface PaymentRepository {

    void save(Payment payment);
}
