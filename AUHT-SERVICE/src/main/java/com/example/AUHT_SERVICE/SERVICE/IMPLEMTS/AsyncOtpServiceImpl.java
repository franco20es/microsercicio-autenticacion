package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import com.example.AUHT_SERVICE.MODEL.ModelUsuario;
import com.example.AUHT_SERVICE.REPOSITORY.RepositoryUsuario;
import com.example.AUHT_SERVICE.SERVICE.AsyncOtpService;
import com.example.AUHT_SERVICE.SERVICE.EmailService;
import com.example.AUHT_SERVICE.SERVICE.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación asincrónica de generación y envío de OTP
 *
 * VENTAJAS:
 * - Login NO espera envío de email
 * - Mejor manejo de errores
 * - Escalable: ejecuta en thread pool separado
 * - Logging detallado de fallos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncOtpServiceImpl implements AsyncOtpService {

    private final OtpService otpService;
    private final EmailService emailService;
    private final RepositoryUsuario userRepository;

    /**
     * Genera y envía OTP de forma ASINCRÓNICA
     * Usa el ThreadPoolTaskExecutor "emailTaskExecutor"
     *
     * FLUJO:
     * 1. Generar código OTP
     * 2. Guardar en BD y Redis
     * 3. Enviar por email (TODO EN BACKGROUND)
     *
     * @param email del usuario
     * @return CompletableFuture completado cuando se envía el email
     */
    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> generateAndSendOtpAsync(String email) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("📧 [ASYNC] Iniciando generación y envío de OTP para: {}", email);

            // Buscar usuario (validación adicional)
            ModelUsuario user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("⚠️ Usuario no encontrado durante envío async: {}", email);
                        return new RuntimeException("Usuario no encontrado");
                    });

            // Generar OTP (sincrónico, rápido)
            otpService.generateAndSendOtp(email);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [ASYNC] OTP enviado exitosamente a {} en {}ms", email, duration);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ [ASYNC] Error en envío de OTP para {} después de {}ms: {}",
                    email, duration, e.getMessage(), e);

            // Manejo asincrónico del error
            handleOtpSendingError(email, e);

            // Retornar future completado (no lanzar excepción en @Async)
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Maneja errores de envío de OTP sin afectar flujo principal
     * Aquí puedes:
     * - Registrar en BD
     * - Reintentarlo
     * - Notificar a admin
     * - Usar queue/mensaje (RabbitMQ, Kafka)
     */
    @Override
    @Async("generalTaskExecutor")
    public void handleOtpSendingError(String email, Exception error) {
        try {
            log.error("🔴 Manejando error de OTP para {}: {}", email, error.getMessage());

            // OPCIÓN 1: Registrar en BD para reintento
            // otpErrorRepository.save(new OtpError(email, error.getMessage(), LocalDateTime.now()));

            // OPCIÓN 2: Guardar en Redis para alertas
            // redisService.recordOtpError(email, error.getMessage());

            // OPCIÓN 3: Notificar admin (solo si es error crítico)
            if (error.getMessage().contains("SMTP")) {
                log.error("🚨 ERROR CRÍTICO DE EMAIL DETECTADO - Notificar admin: {}", email);
                // notificationService.alertAdmin("Email service down", error);
            }

        } catch (Exception e) {
            log.error("⚠️ Error en manejador de errores de OTP: {}", e.getMessage(), e);
        }
    }
}

