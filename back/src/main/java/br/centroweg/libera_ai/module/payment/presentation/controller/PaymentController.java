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
    public Flux<ServerSentEvent<String>> streamPaymentStatus(@PathVariable String paymentId) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> {
                    boolean isPaid = getPaymentStatusUseCase.execute(paymentId);
                    return ServerSentEvent.<String>builder()
                            .data(String.valueOf(isPaid))
                            .build();
                })
                .doOnError(e -> System.err.println("Erro no stream: " + e.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }

    /**
     * Webhook endpoint for Mercado Pago payment notifications.
     * 
     * Security measures:
     * 1. Returns 200 immediately to prevent MP retries
     * 2. Validates webhook type before processing
     * 3. Processes asynchronously to not block response
     * 4. The UseCase validates payment by fetching from MP API (prevents bypass attacks)
     * 5. Idempotency is handled in the UseCase layer
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody MercadoPagoWebhookRequest request) {
        log.info("[WEBHOOK] Received notification - Type: {}, Action: {}, Data ID: {}", 
                request.type(), request.action(), request.data() != null ? request.data().paymentId() : "null");

        // Step 1: Return 200 immediately as recommended by Mercado Pago
        // This prevents MP from sending retries while we process
        
        // Step 2: Validate the webhook type - only process payment notifications
        if (request.type() == null || !request.type().equals("payment")) {
            log.info("[WEBHOOK] Ignoring non-payment notification type: {}", request.type());
            return ResponseEntity.ok().build();
        }

        // Step 3: Validate required data
        if (request.data() == null || request.data().paymentId() == null || request.data().paymentId().isBlank()) {
            log.warn("[WEBHOOK] Received notification with missing data or payment ID");
            return ResponseEntity.ok().build();
        }

        String paymentId = request.data().paymentId();

        // Step 4: Process asynchronously to ensure we return 200 quickly
        // This is important because MP expects a quick response
        CompletableFuture.runAsync(() -> {
            try {
                processPaymentNotificationUseCase.execute(paymentId);
            } catch (Exception e) {
                log.error("[WEBHOOK] Error processing notification for payment {}: {}", paymentId, e.getMessage(), e);
            }
        });

        return ResponseEntity.ok().build();
    }

}
