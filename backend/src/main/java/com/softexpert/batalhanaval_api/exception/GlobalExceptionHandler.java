package com.softexpert.batalhanaval_api.exception;

import com.softexpert.batalhanaval_api.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> {
                String field = error.getField();
                String defaultMsg = error.getDefaultMessage();
                return switch (field) {
                    case "email" -> "Email inválido";
                    case "password" -> defaultMsg != null && !defaultMsg.isBlank() ? defaultMsg : "Senha inválida";
                    case "username" -> "Usuário deve ter entre 3 e 30 caracteres (apenas letras, números e _)";
                    default -> field + ": " + defaultMsg;
                };
            })
            .distinct()
            .collect(Collectors.joining("; "));
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity
            .internalServerError()
            .body(ErrorResponse.of("INTERNAL_ERROR", "Ocorreu um erro inesperado"));
    }
}
