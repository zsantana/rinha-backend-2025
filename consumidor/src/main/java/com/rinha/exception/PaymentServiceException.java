package com.rinha.exception;

public class PaymentServiceException extends RuntimeException {
    
    private final int statusCode;
    
    public PaymentServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public PaymentServiceException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}