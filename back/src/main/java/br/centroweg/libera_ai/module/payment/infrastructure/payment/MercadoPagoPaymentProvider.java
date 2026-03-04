package br.centroweg.libera_ai.module.payment.infrastructure.payment;

import br.centroweg.libera_ai.module.payment.domain.model.PaymentInfo;
import br.centroweg.libera_ai.module.payment.domain.port.PaymentProvider;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;

@Slf4j // Usando apenas o Lombok para manter o código limpo
@Service
public class MercadoPagoPaymentProvider implements PaymentProvider {

    private final PaymentClient paymentClient;
    private final String accessToken;
    private final String notificationUrl;

    public MercadoPagoPaymentProvider(
            @Value("${mercadopago.access-token}") String accessToken,
            @Value("${mercadopago.notification-url:}") String notificationUrl
    ) {
        log.info("[MP-CONFIG] Inicializando provedor Mercado Pago. Notification URL: {}", notificationUrl);
        MercadoPagoConfig.setAccessToken(accessToken);
        this.accessToken = accessToken;
        this.paymentClient = new PaymentClient();
        this.notificationUrl = notificationUrl;
    }

    public PaymentInfo generatePayment(double amount, String internalPaymentId) {
        log.info("[MP-GENERATE] Criando preferência para ID Interno: {} | Valor: {}", internalPaymentId, amount);

        try {
            PreferenceClient client = new PreferenceClient();

            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .id(internalPaymentId)
                    .title("Libera AI - Acesso " + internalPaymentId)
                    .quantity(1)
                    .unitPrice(new BigDecimal(String.valueOf(amount)))
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("http://localhost:3000/")
                    .failure("http://localhost:3000/")
                    .pending("http://localhost:3000/")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .externalReference(internalPaymentId)
                    .notificationUrl(notificationUrl)
//                    .backUrls(backUrls)
//                    .autoReturn("approved")
                    .build();

            Preference preference = client.create(preferenceRequest);

            log.info("[MP-GENERATE] Sucesso! MP-Preference-ID: {} vinculada ao Internal-ID: {}",
                    preference.getId(), internalPaymentId);

            return new PaymentInfo(
                    preference.getId(),
                    preference.getSandboxInitPoint(),
                    amount
            );

        } catch (MPApiException e) {
            String apiResponse = (e.getApiResponse() != null) ? e.getApiResponse().getContent() : "Sem conteúdo";
            log.error("[MP-GENERATE] Erro da API do Mercado Pago: {}", apiResponse);
            throw new RuntimeException("Erro na API MP: " + apiResponse);
        } catch (MPException e) {
            log.error("[MP-GENERATE] Erro no SDK do Mercado Pago: {}", e.getMessage());
            throw new RuntimeException("Erro SDK MP: " + e.getMessage());
        } catch (Exception e) {
            log.error("[MP-GENERATE] Erro inesperado: {}", e.getMessage(), e);
            throw new RuntimeException("Erro inesperado: " + e.getMessage());
        }
    }

    @Override
    public String fetchStatus(String mercadoPagoPaymentId) {
        try {
            Payment payment = getPaymentFromMP(mercadoPagoPaymentId);
            log.info("[MP-STATUS] ID MP: {} | Status: {} | Detalhe: {}",
                    mercadoPagoPaymentId, payment.getStatus(), payment.getStatusDetail());
            return payment.getStatus();
        } catch (Exception e) {
            log.error("[MP-STATUS] Falha ao buscar status do pagamento MP: {}", mercadoPagoPaymentId);
            return "pending";
        }
    }

    @Override
    public String getExternalReference(String mercadoPagoPaymentId) {
        log.info("[MP-WEBHOOK] Iniciando extração de UUID para o pagamento MP: {}", mercadoPagoPaymentId);

        try {
            Payment payment = getPaymentFromMP(mercadoPagoPaymentId);
            String externalRef = payment.getExternalReference();

            if (externalRef == null) {
                log.error("[MP-WEBHOOK] CRÍTICO: O Mercado Pago retornou external_reference NULO para o pagamento {}. " +
                        "Isso significa que o ID interno não foi enviado corretamente na criação.", mercadoPagoPaymentId);
                return null;
            }

            log.info("[MP-WEBHOOK] UUID Recuperado: '{}' (Tamanho: {}) para MP-ID: {}",
                    externalRef, externalRef.length(), mercadoPagoPaymentId);

            return externalRef;
        } catch (Exception e) {
            log.error("[MP-WEBHOOK] Erro ao recuperar referência externa para MP-ID: {}", mercadoPagoPaymentId, e);
            return null;
        }
    }

    private Payment getPaymentFromMP(String paymentId) throws MPException, MPApiException {
        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .accessToken(accessToken)
                .build();
        return paymentClient.get(Long.valueOf(paymentId), requestOptions);
    }

    private String getApiResponseContent(MPApiException e) {
        return (e.getApiResponse() != null) ? e.getApiResponse().getContent() : "Sem conteúdo de resposta";
    }
}