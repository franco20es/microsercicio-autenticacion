package com.example.AUHT_SERVICE.EXCEPTION;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Manejar errores de validación en DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        log.warn("Errores de validación: {}", errors);
        return errors;
    }

    // ===== EXCEPCIONES ESPECÍFICAS DE 2FA =====

    /**
     * 2FA: Secret no configurado (usuario no ha escaneado el QR)
     */
    @ExceptionHandler(TwoFactorSecretNotConfiguredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleTwoFactorSecretNotConfigured(TwoFactorSecretNotConfiguredException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("errorCode", "2FA_SECRET_NOT_CONFIGURED");
        error.put("errorMessage", ex.getMessage());
        log.error(" Error 2FA (secret no configurado): {}", ex.getMessage());
        return error;
    }

    /**
     * 2FA: Código con formato inválido (no es exactamente 6 dígitos)
     */
    @ExceptionHandler(TwoFactorCodeFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleTwoFactorCodeFormat(TwoFactorCodeFormatException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("errorCode", "2FA_CODE_INVALID_FORMAT");
        error.put("errorMessage", ex.getMessage());
        log.error(" Error 2FA (formato de código): {}", ex.getMessage());
        return error;
    }

    /**
     * 2FA: Código incorrecto o expirado (no coincide con ninguna ventana de tiempo)
     */
    @ExceptionHandler(TwoFactorInvalidCodeException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleTwoFactorInvalidCode(TwoFactorInvalidCodeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("errorCode", "2FA_CODE_INVALID_OR_EXPIRED");
        error.put("errorMessage", ex.getMessage());
        log.error(" Error 2FA (código inválido/expirado): {}", ex.getMessage());
        return error;
    }

    // ===== EXCEPCIONES GENÉRICAS =====

    // Manejar excepciones de tipo RuntimeException (último recurso)
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage();
        
        // Detectar si es un error de bloqueo de cuenta
        if (message != null && message.contains("bloqueada")) {
            error.put("errorCode", "ACCOUNT_LOCKED");
            error.put("error", message);
            log.error(" CUENTA BLOQUEADA: {}", message);
        } 
        // Detectar si es un error de credenciales
        else if (message != null && message.contains("Credenciales inválidas")) {
            error.put("errorCode", "INVALID_CREDENTIALS");
            error.put("error", message);
            log.warn("  CREDENCIALES INVÁLIDAS: {}", message);
        }
        // Otros errores
        else {
            error.put("error", message != null ? message : "Error desconocido");
            log.error(" RuntimeException: {}", message);
        }
        
        return error;
    }

    // ===== EMAIL DUPLICADO EN REGISTRO =====

    /**
     * Email duplicado al intentar registrarse con Google
     * Retorna HTTP 400 Bad Request con mensaje claro
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleDuplicateEmail(IllegalArgumentException ex) {
        Map<String, Object> error = new HashMap<>();
        String message = ex.getMessage();

        // Detectar si es error de email duplicado
        if (message != null && message.contains("ya está registrado")) {
            error.put("statusCode", 400);
            error.put("message", message);
            error.put("errorCode", "DUPLICATE_EMAIL");
            log.warn(" INTENTO DE REGISTRO CON EMAIL DUPLICADO: {}", message);
        } else {
            error.put("message", message != null ? message : "Error de validación");
            log.warn(" Error de validación: {}", message);
        }

        return error;
    }

    /**
     * Email duplicado (excepción personalizada)
     */
    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleDuplicateEmailException(DuplicateEmailException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", 400);
        error.put("message", ex.getMessage());
        error.put("errorCode", "DUPLICATE_EMAIL");
        
        log.warn("REGISTRO CON EMAIL DUPLICADO: {}", ex.getMessage());
        return error;
    }
}