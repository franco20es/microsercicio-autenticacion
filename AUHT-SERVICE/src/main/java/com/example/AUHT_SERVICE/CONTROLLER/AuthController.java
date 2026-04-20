package com.example.AUHT_SERVICE.CONTROLLER;

import com.example.AUHT_SERVICE.DTO.Request.*;
import com.example.AUHT_SERVICE.SERVICE.AuthService;
import com.example.AUHT_SERVICE.SERVICE.OtpService;
import com.example.AUHT_SERVICE.SERVICE.TwoFactorService;
import com.example.AUHT_SERVICE.SERVICE.GoogleAuthService;
import com.example.AUHT_SERVICE.SERVICE.PasswordService;
import com.example.AUHT_SERVICE.SERVICE.RedisService;
import com.example.AUHT_SERVICE.UTILS.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.AUHT_SERVICE.DTO.Response.AuthResponse;
import com.example.AUHT_SERVICE.DTO.Response.GoogleLoginResponse;
import com.example.AUHT_SERVICE.DTO.Response.MessageResponse;
import com.example.AUHT_SERVICE.DTO.Response.QrResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    // DEV:  http://localhost:8081/api/auth/otp
    // PROD: https://tu-dominio.com/api/auth/otp

    private final AuthService authService;
    private final OtpService otpService;
    private final TwoFactorService twoFactorService;
    private final GoogleAuthService googleAuthService;
    private final PasswordService passwordService;
    private final RedisService redisService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/otp")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpRequest request) {
        return otpService.verifyOtp(request);
    }

    @PostMapping("/2fa")
    public AuthResponse verify2FA(@Valid @RequestBody TwoFactorRequest request) {
        return twoFactorService.verify2FA(request);
    }

    @GetMapping("/qr")
    public QrResponse generateQr(@Valid @RequestParam String email) {
        return twoFactorService.generateQr(email);
    }

    @PostMapping("/google/login")
    public GoogleLoginResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return googleAuthService.loginWithGoogle(request);
    }

    @PostMapping("/google/register")
    public GoogleLoginResponse registerWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return googleAuthService.registerWithGoogle(request);
    }

    @PostMapping("/recuperar-password")
    public MessageResponse recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest request) {
        return passwordService.recuperarPassword(request);
    }

    @PostMapping("/cambiar-password")
    public MessageResponse changePassword(@Valid @RequestBody CambiarPasswordRequest request) {
        return passwordService.changePassword(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extraer token sin "Bearer "
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Token no válido");
            }

            String token = authHeader.substring(7);

            // Obtener tiempo de expiración del JWT
            long expirySeconds = jwtUtil.getExpirationSeconds(token);

            //  AÑADIR TOKEN A BLACKLIST EN REDIS
            redisService.blacklistToken(token, expirySeconds);

            log.info(" Logout exitoso - Token añadido a blacklist");

            return MessageResponse.builder()
                    .message("Logout exitoso")
                    .build();

        } catch (Exception e) {
            log.error(" Error en logout: {}", e.getMessage());
            throw new RuntimeException("Error en logout: " + e.getMessage());
        }
    }
}