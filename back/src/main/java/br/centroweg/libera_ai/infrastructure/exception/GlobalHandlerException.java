package br.centroweg.libera_ai.infrastructure.exception;

import br.centroweg.libera_ai.module.access.domain.exception.AccessDomainException;
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



}

