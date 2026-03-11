package br.centroweg.open_it.share.exception;

import br.centroweg.open_it.module.access.domain.exception.AccessDomainException;
import br.centroweg.open_it.module.access.infrastructure.exception.IoTIntegrationException;
import br.centroweg.open_it.module.payment.infrastructure.exception.PaymentIntegrationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalHandlerException {


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex) {
        var status = HttpStatus.BAD_REQUEST.value();

        var response = ApiErrorResponse.of(
                status,
                "Runtime exception: erro insperado ocorreu. ",
                ex.getMessage()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(AccessDomainException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDomainException(AccessDomainException ex) {
        var status = HttpStatus.BAD_REQUEST.value();

        var response = ApiErrorResponse.of(
                status,
                "Access domain exception: uma violação de négocio ocorreu. ",
                ex.getMessage()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(IoTIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleIoTIntegrationException(IoTIntegrationException ex) {
        var status = HttpStatus.BAD_GATEWAY.value();

        var response = ApiErrorResponse.of(
                status,
                "IoT integration error: failed to communicate with Node-RED",
                ex.getMessage()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(PaymentIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentIntegrationException(PaymentIntegrationException ex) {
        var status = HttpStatus.BAD_GATEWAY.value();

        var response = ApiErrorResponse.of(
                status,
                "Erro de integração com provedor de pagamento",
                ex.getMessage()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

}

