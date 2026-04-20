package com.example.AUHT_SERVICE.EXCEPTION;

/**
 * Excepción para cuando se intenta registrar un email duplicado
 * Será capturada por GlobalExceptionHandler y convertida a HTTP 400
 */
public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String message) {
        super(message);
    }

    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}

