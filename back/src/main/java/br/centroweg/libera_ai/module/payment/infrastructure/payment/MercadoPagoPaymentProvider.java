package br.centroweg.libera_ai.module.payment.infrastructure.payment;

import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentProvider;
import br.centroweg.libera_ai.module.payment.infrastructure.exception.PaymentIntegrationException;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MercadoPagoPaymentProvider implements PaymentProvider {

    private final PaymentClient paymentClient;
    private final String defaultEmail;

    public MercadoPagoPaymentProvider(
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.default-payer-email:parking@libera.ai.com}") String defaultEmail) {

        MercadoPagoConfig.setAccessToken(accessToken);
        this.paymentClient = new PaymentClient();
        this.defaultEmail = defaultEmail;
    }

    @Override
    public PaymentInfo generatePayment(double amount) {
        try {
            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .transactionAmount(BigDecimal.valueOf(amount))
                    .paymentMethodId("pix")
                    .payer(PaymentPayerRequest.builder()
                            .email(defaultEmail)
                            .build())
                    .build();

            Payment payment = paymentClient.create(request);

            String generatedPaymentId = String.valueOf(payment.getId());
            String qrCode = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();

            return new PaymentInfo(generatedPaymentId, qrCode, amount);

        } catch (MPException | MPApiException e) {
            throw new PaymentIntegrationException("Failed to generate Pix payment with Mercado Pago", e);
        }
    }

    @Override
    public String fetchStatus(String externalId) {
        try {
            Long mpId = Long.valueOf(externalId);

            Payment payment = paymentClient.get(mpId);

            return payment.getStatus();

        } catch (MPException | MPApiException e) {
            throw new PaymentIntegrationException("Failed to verify payment status with Mercado Pago", e);
        }
    }
}