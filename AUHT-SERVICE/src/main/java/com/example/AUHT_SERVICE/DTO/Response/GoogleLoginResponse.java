package com.example.AUHT_SERVICE.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO específico para la respuesta de login con Google,
// que incluye el token JWT generado por el backend
// y la información del usuario Google.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginResponse {
    private String accessToken;    // JWT generado por el backend
    private String email;          // Email del usuario Google
    private String nombre;         // Nombre del usuario Google
    private String rol;            // Rol asignado (siempre ROLE_USER para Google)
    private String proveedor;      // "google"
    private boolean twoFactorRequired; // Siempre false para Google Login
}
