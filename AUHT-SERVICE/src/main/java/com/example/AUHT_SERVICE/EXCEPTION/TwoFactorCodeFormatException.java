package com.example.AUHT_SERVICE.EXCEPTION;

/**
 * Excepción lanzada cuando el formato del código 2FA es inválido
 * (no es exactamente 6 dígitos numéricos)
 */
public class TwoFactorCodeFormatException extends RuntimeException {

    public TwoFactorCodeFormatException(String message) {
        super(message);
    }

    public TwoFactorCodeFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

