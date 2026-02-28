package br.centroweg.libera_ai.module.payment.domain.model;

public record PaymentInfo(
        String generatedPaymentId,
        String qrCode
) {
}
