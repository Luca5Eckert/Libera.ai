package br.centroweg.libera_ai.module.payment.presentation.dto;

public record PaymentResponse(
        double amount,
        String linkPayment,
        String paymentId,
        String qrCode,
        String qrCodeBase64,
        String ticketUrl
) {
}
