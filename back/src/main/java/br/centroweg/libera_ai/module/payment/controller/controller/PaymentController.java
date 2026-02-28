package br.centroweg.libera_ai.module.payment.controller.controller;

import br.centroweg.libera_ai.module.payment.controller.dto.CreatePaymentRequest;
import br.centroweg.libera_ai.module.payment.controller.dto.PaymentResponse;
import br.centroweg.libera_ai.module.payment.controller.mapper.PaymentMapper;
import br.centroweg.libera_ai.module.payment.controller.use_case.CreatePaymentUseCase;
import br.centroweg.libera_ai.module.payment.controller.use_case.GetPaymentStatusUseCase;
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

    private final PaymentMapper mapper;

    public PaymentController(CreatePaymentUseCase createPaymentUseCase, GetPaymentStatusUseCase getPaymentStatusUseCase, PaymentMapper mapper) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.getPaymentStatusUseCase = getPaymentStatusUseCase;
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

}
