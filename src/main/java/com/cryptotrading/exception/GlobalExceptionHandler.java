package com.cryptotrading.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CoinApiException.class)
    public ResponseEntity<ErrorResponse> handleCoinApiException(CoinApiException ex) {
        log.error("Coin API Error: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "Coin API Error",
                ex.getMessage()
        );
    }

    @ExceptionHandler(GeminiApiException.class)
    public ResponseEntity<ErrorResponse> handleGeminiApiException(GeminiApiException ex) {
        log.error("Gemini API Error: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "Gemini API Error",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Bad Request (IllegalArgumentException): {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.warn("State Error (IllegalStateException): {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid State",
                ex.getMessage()
        );
    }

    @ExceptionHandler(VerificationCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVerificationCodeNotFound(VerificationCodeNotFoundException ex) {
        log.warn("Verification Code Not Found: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Verification Code Not Found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationCode(InvalidVerificationCodeException ex) {
        log.warn("Invalid Verification Code: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Verification Code",
                ex.getMessage()
        );
    }

    @ExceptionHandler(PaymentOrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentOrderNotFound(PaymentOrderNotFoundException ex) {
        log.warn("Payment Order Not Found: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Payment Order Not Found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        log.warn("Validation Error: {}", message);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                message
        );
    }

    @ExceptionHandler(PaymentOwnershipException.class)
    public ResponseEntity<ErrorResponse> handlePaymentOwnership(PaymentOwnershipException ex) {
        log.warn("Payment Ownership Error: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Payment Ownership Error",
                ex.getMessage()
        );
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentVerification(PaymentVerificationException ex) {
        log.warn("Payment Verification Failed: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Payment Verification Failed",
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayment(InvalidPaymentException ex) {
        log.warn("Invalid Payment: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Payment",
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled Internal Server Error: ", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Something went wrong"
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message) {

        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(status)
                .body(response);
    }
}