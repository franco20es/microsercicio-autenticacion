package com.example.AUHT_SERVICE.DTO.Response;

import lombok.Builder;
import lombok.Data;
// DTO para la respuesta de autenticación,
// que incluye tokens, información del usuario y
// si se requiere 2FA.
@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    private String email;
    private String rol;

    private boolean twoFactorRequired;
}