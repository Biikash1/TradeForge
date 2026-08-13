package com.cryptotrading.exception;

import com.cryptotrading.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CoinApiException.class)
    public ResponseEntity<Map<String, Object>> handleCoinApiException(
            CoinApiException ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.BAD_GATEWAY.value());
        response.put("error", "Coin API Error");
        response.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "Something went wrong");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(
            VerificationCodeNotFoundException.class
    )
    public ResponseEntity<ApiResponse> handleVerificationCodeNotFound(
            VerificationCodeNotFoundException exception
    ) {

        ApiResponse response = new ApiResponse();

        response.setMessage(exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(
            InvalidVerificationCodeException.class
    )
    public ResponseEntity<ApiResponse> handleInvalidVerificationCode(
            InvalidVerificationCodeException exception
    ) {

        ApiResponse response = new ApiResponse();

        response.setMessage(exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(PaymentOrderNotFoundException.class)
    public ResponseEntity<String> handlePaymentOrderNotFound(
            PaymentOrderNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(PaymentOwnershipException.class)
    public ResponseEntity<String> handlePaymentOwnership(
            PaymentOwnershipException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ex.getMessage());
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<String> handlePaymentVerification(
            PaymentVerificationException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayment(
            InvalidPaymentException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message) {

        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(status)
                .body(response);
    }
}