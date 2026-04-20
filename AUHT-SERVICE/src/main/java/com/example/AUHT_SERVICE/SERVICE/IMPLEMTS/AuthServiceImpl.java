package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import com.example.AUHT_SERVICE.DTO.Request.*;
import com.example.AUHT_SERVICE.SERVICE.*;
import com.example.AUHT_SERVICE.UTILS.PasswordSeguroUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.AUHT_SERVICE.DTO.Response.AuthResponse;
import com.example.AUHT_SERVICE.DTO.Response.MessageResponse;
import com.example.AUHT_SERVICE.MODEL.ModelRoles;
import com.example.AUHT_SERVICE.MODEL.ModelUsuario;
import com.example.AUHT_SERVICE.REPOSITORY.RepositoryUsuario;
import com.example.AUHT_SERVICE.UTILS.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final RepositoryUsuario userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final OtpService otpService;
    private final AsyncOtpService asyncOtpService;
    private final RedisService redisService;

    @Override
    public MessageResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        if (!PasswordSeguroUtil.validatePassword(request.getPassword())) {
            throw new RuntimeException(PasswordSeguroUtil.GetError());
        }

        ModelUsuario user = ModelUsuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(ModelRoles.ROLE_USER)
                .activo(true)
                .bloqueado(false)
                .fechaCreacion(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return new MessageResponse("Usuario registrado correctamente");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        long startTime = System.currentTimeMillis();
        String email = request.getEmail();

        try {
            // ========== VALIDACIÓN RÁPIDA (SÍNCRONA) ==========

            // 1. Buscar usuario en BD (con índice en email)
            ModelUsuario user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        recordFailedLoginMetric(email);
                        return new RuntimeException("Usuario no encontrado");
                    });

            // 2. Validar bloqueo de cuenta EN REDIS (1ms típicamente)
            if (redisService.isAccountLocked(email)) {
                long intentos = redisService.getFailedLoginAttempts(email);
                log.error("🔒 CUENTA BLOQUEADA: {} (Intentos: {}/5)", email, intentos);
                throw new RuntimeException("Cuenta bloqueada por 30 minutos. Intenta más tarde.");
            }

            // 3. Validar contraseña CON BCRYPT (rápido con factor configurado)
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                recordFailedLogin(email);
                throw new RuntimeException("Credenciales inválidas. Intenta de nuevo.");
            }

            // 4. Validar estado de cuenta
            if (!user.isActivo()) {
                throw new RuntimeException("Cuenta desactivada");
            }

            // ========== LOGIN EXITOSO ==========
            long syncDuration = System.currentTimeMillis() - startTime;
            log.info("✅ LOGIN EXITOSO ({}ms): {}", syncDuration, email);

            // 5. LIMPIAR INTENTOS EN REDIS (sincrónico, muy rápido)
            redisService.clearFailedLoginAttempts(email);

            // 6. ⚡ ENVÍO DE OTP ASINCRÓNICO - NO BLOQUEA RESPUESTA
            // Se ejecuta en background, permite que login responda inmediatamente
            asyncOtpService.generateAndSendOtpAsync(email)
                    .thenRun(() -> log.debug("📧 OTP iniciado en background para: {}", email))
                    .exceptionally(ex -> {
                        log.warn("⚠️ Error en envío async de OTP (no bloquea login): {}", ex.getMessage());
                        return null;
                    });

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("⏱️ Tiempo de respuesta login: {}ms (sin esperar email)", totalDuration);

            // ========== RESPUESTA INMEDIATA ==========
            return AuthResponse.builder()
                    .twoFactorRequired(true)
                    .email(user.getEmail())
                    .build();

        } catch (RuntimeException e) {
            long errorDuration = System.currentTimeMillis() - startTime;
            log.warn("❌ Login fallido ({}ms): {} - {}", errorDuration, email, e.getMessage());
            throw e;
        }
    }

    /**
     * Registra un intento fallido de login en Redis
     * OPTIMIZACIÓN: Una sola llamada a Redis
     */
    private void recordFailedLogin(String email) {
        redisService.recordFailedLogin(email);
        long intentos = redisService.getFailedLoginAttempts(email);
        long intentosRestantes = 5 - intentos;

        log.warn("⚠️ INTENTO FALLIDO: {} - Intentos: {}/5 (Restantes: {})", email, intentos, intentosRestantes);

        // Bloquear si se alcanzó el límite
        if (intentos >= 5) {
            log.error("🔴 CUENTA BLOQUEADA: {} después de 5 intentos", email);
            throw new RuntimeException("Cuenta bloqueada por 30 minutos. Intenta más tarde.");
        }

        if (intentosRestantes <= 2) {
            log.warn("⚠️ ALERTA: {} solo tiene {} intentos restantes", email, intentosRestantes);
        }
    }

    /**
     * Registra métrica de login fallido (para monitoreo)
     */
    private void recordFailedLoginMetric(String email) {
        // Aquí puedes integrar Micrometer, Prometheus, etc.
        // metricsService.recordFailedLogin(email);
        log.debug("📊 Métrica de login fallido registrada para: {}", email);
    }

}




