package br.centroweg.libera_ai.module.payment.controller.controller;

import br.centroweg.libera_ai.module.payment.controller.dto.CreatePaymentRequest;
import br.centroweg.libera_ai.module.payment.controller.dto.PaymentResponse;
import br.centroweg.libera_ai.module.payment.controller.mapper.PaymentMapper;
import br.centroweg.libera_ai.module.payment.controller.use_case.CreatePaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;

    private final PaymentMapper mapper;

    public PaymentController(CreatePaymentUseCase createPaymentUseCase, PaymentMapper mapper) {
        this.createPaymentUseCase = createPaymentUseCase;
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

}
