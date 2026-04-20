package com.example.AUHT_SERVICE.SERVICE;

public interface TokenService {

    // Metodo que  Genera un token de acceso con el email y rol del usuario
    String generateAccessToken(String email, String rol);

    // Metodo que Genera un token de refresco con el email del usuario
    String generateRefreshToken(String email);

    // Metodo que Valida un token de acceso o refresco
    boolean validateToken(String token);

    // Metodo que Extrae el email del usuario a partir del token
    String extractEmail(String token);
}
