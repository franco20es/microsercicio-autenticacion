package com.example.AUHT_SERVICE.EXCEPTION;

/**
 * Excepción lanzada cuando el usuario no ha configurado Google Authenticator aún
 * (el secret 2FA no está en la base de datos)
 */
public class TwoFactorSecretNotConfiguredException extends RuntimeException {

    public TwoFactorSecretNotConfiguredException(String message) {
        super(message);
    }

    public TwoFactorSecretNotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }
}

