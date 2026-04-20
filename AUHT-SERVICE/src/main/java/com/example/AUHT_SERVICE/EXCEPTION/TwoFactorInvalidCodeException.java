package com.example.AUHT_SERVICE.EXCEPTION;

/**
 * Excepción lanzada cuando el código TOTP ingresado es incorrecto o ha expirado
 * (no coincide en ninguna ventana de tiempo permitida)
 */
public class TwoFactorInvalidCodeException extends RuntimeException {

    public TwoFactorInvalidCodeException(String message) {
        super(message);
    }

    public TwoFactorInvalidCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}

