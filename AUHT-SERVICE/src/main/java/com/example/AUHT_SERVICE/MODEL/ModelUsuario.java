package com.example.AUHT_SERVICE.MODEL;


import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //  DATOS BÁSICOS
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "nombre")
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModelRoles rol;

    //  ESTADO DE LA CUENTA
    @Builder.Default
    private boolean activo = true;
    @Builder.Default
    private boolean bloqueado = false;

    //  CONTROL DE INTENTOS
    @Builder.Default
    private Integer intentosFallidos = 0;
    private LocalDateTime bloqueadoHasta;

    //  VERIFICACIÓN EMAIL
    @Builder.Default
    private boolean emailVerificado = false;
    private String tokenVerificacion;

    //  OTP POR EMAIL
    private String codigoOtp;
    private LocalDateTime expiracionOtp;

    //  GOOGLE AUTHENTICATOR (TOTP)
    @Builder.Default
    private boolean twoFactorEnabled = false;
    private String secret2FA;

    //  RECUPERACIÓN DE CONTRASEÑA
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;

    //  AUDITORÍA (MUY IMPORTANTE)
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime ultimoLogin;
}