package br.centroweg.libera_ai.module.payment.presentation.controller;

import br.centroweg.libera_ai.module.payment.application.use_case.ProcessPaymentNotificationUseCase;
import br.centroweg.libera_ai.module.payment.presentation.dto.CreatePaymentRequest;
import br.centroweg.libera_ai.module.payment.presentation.dto.MercadoPagoWebhookRequest;
import br.centroweg.libera_ai.module.payment.presentation.dto.PaymentResponse;
import br.centroweg.libera_ai.module.payment.presentation.mapper.PaymentMapper;
import br.centroweg.libera_ai.module.payment.application.use_case.CreatePaymentUseCase;
import br.centroweg.libera_ai.module.payment.application.use_case.GetPaymentStatusUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentStatusUseCase getPaymentStatusUseCase;
    private final ProcessPaymentNotificationUseCase processPaymentNotificationUseCase;

    private final PaymentMapper mapper;

    public PaymentController(CreatePaymentUseCase createPaymentUseCase, GetPaymentStatusUseCase getPaymentStatusUseCase, ProcessPaymentNotificationUseCase processPaymentNotificationUseCase, PaymentMapper mapper) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.getPaymentStatusUseCase = getPaymentStatusUseCase;
        this.processPaymentNotificationUseCase = processPaymentNotificationUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody @Valid CreatePaymentRequest request
    ) {
        var paymentInfo = createPaymentUseCase.execute(request.accessCode());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toPaymentResponse(paymentInfo));
    }

    @GetMapping(path = "/stream/{paymentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Boolean> streamPaymentStatus(@PathVariable String paymentId) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> getPaymentStatusUseCase.execute(paymentId))
                .distinctUntilChanged();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody MercadoPagoWebhookRequest request) {
        String paymentId = request.data().paymentId();

        processPaymentNotificationUseCase.execute(paymentId);

        return ResponseEntity.ok().build();
    }

}
