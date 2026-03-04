package br.centroweg.libera_ai.module.payment.presentation.controller;

import br.centroweg.libera_ai.module.payment.application.use_case.ProcessPaymentNotificationUseCase;
import br.centroweg.libera_ai.module.payment.presentation.dto.CreatePaymentRequest;
import br.centroweg.libera_ai.module.payment.presentation.dto.MercadoPagoWebhookRequest;
import br.centroweg.libera_ai.module.payment.presentation.dto.PaymentResponse;
import br.centroweg.libera_ai.module.payment.presentation.mapper.PaymentMapper;
import br.centroweg.libera_ai.module.payment.application.use_case.CreatePaymentUseCase;
import br.centroweg.libera_ai.module.payment.application.use_case.GetPaymentStatusUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final CreatePaymentUseCase createUseCase;
    private final GetPaymentStatusUseCase statusUseCase;
    private final ProcessPaymentNotificationUseCase processNotificationUseCase;

    private final PaymentMapper mapper;

    public PaymentController(CreatePaymentUseCase createUseCase, GetPaymentStatusUseCase statusUseCase, ProcessPaymentNotificationUseCase processNotificationUseCase, PaymentMapper mapper) {
        this.createUseCase = createUseCase;
        this.statusUseCase = statusUseCase;
        this.processNotificationUseCase = processNotificationUseCase;
        this.mapper = mapper;
    }


    @PostMapping
    public ResponseEntity<PaymentResponse> create(@RequestBody @Valid CreatePaymentRequest request) {
        var paymentInfo = createUseCase.execute(request.accessCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toPaymentResponse(paymentInfo));
    }

    @GetMapping(path = "/stream/{paymentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamStatus(@PathVariable String paymentId) {
        return Flux.interval(Duration.ofSeconds(2))
                .map(tick -> statusUseCase.execute(paymentId))
                .map(isPaid -> ServerSentEvent.<String>builder().data(String.valueOf(isPaid)).build())
                .takeUntil(sse -> "true".equals(sse.data()));
    }


    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody MercadoPagoWebhookRequest request) {
        if ("payment".equals(request.type()) && request.data() != null) {
            CompletableFuture.runAsync(() -> processNotificationUseCase.execute(request.data().paymentId()));
        }
        return ResponseEntity.ok().build();
    }

}
