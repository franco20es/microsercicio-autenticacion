package com.example.AUHT_SERVICE.SERVICE;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio separado para operaciones asincrónicas de OTP
 * Desacopla la lógica de envío del flujo principal
 */
public interface AsyncOtpService {

    /**
     * Genera y envía OTP de forma ASINCRÓNICA
     * NO bloquea el thread principal
     *
     * @param email del usuario
     * @return CompletableFuture para tracking opcional
     */
    CompletableFuture<Void> generateAndSendOtpAsync(String email);

    /**
     * Manejo asincrónico de fallos en envío de OTP
     * Registra errores en logs sin afectar flujo de login
     */
    void handleOtpSendingError(String email, Exception error);
}

