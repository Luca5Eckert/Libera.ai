package br.centroweg.libera_ai.module.payment.controller.dto;

public record PaymentResponse(
        double amount,
        String qrCode
) {
}
