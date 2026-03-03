package br.centroweg.libera_ai.module.payment.domain.model;

public record PaymentInfo(
        String generatedPaymentId,
        String linkPayment,
        double amount,
        String qrCode,
        String qrCodeBase64,
        String ticketUrl
) {
}
