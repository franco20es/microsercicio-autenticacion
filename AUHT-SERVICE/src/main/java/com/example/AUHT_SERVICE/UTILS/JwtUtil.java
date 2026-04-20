package com.example.AUHT_SERVICE.UTILS;


import java.security.Key;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private String jwtExpirationString;

    private long getJwtExpiration() {
        try {
            return Long.parseLong(jwtExpirationString);
        } catch (Exception e) {
            return 86400000L; // 24 horas por defecto
        }
    }

    //  Obtener key segura
    private Key getKey() {
        byte[] decodedKey = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(decodedKey);
    }

    //  GENERAR TOKEN CON ROL
    public String generateToken(String email, String rol) {
        return Jwts.builder()
                .setSubject(email)
                .claim("rol", rol) //  AQUÍ VA EL ROL
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + getJwtExpiration()))
                .signWith(getKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    //  EXTRAER EMAIL
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    //  EXTRAER ROL
    public String getRolFromToken(String token) {
        return getClaims(token).get("rol", String.class);
    }

    //  VALIDAR TOKEN
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // OBTENER TIEMPO DE EXPIRACIÓN EN SEGUNDOS (para Redis blacklist)
    public long getExpirationSeconds(String token) {
        try {
            Claims claims = getClaims(token);
            Date expirationDate = claims.getExpiration();
            long expirationTimeMs = expirationDate.getTime();
            long currentTimeMs = System.currentTimeMillis();
            long remainingMs = expirationTimeMs - currentTimeMs;
            return Math.max(0, remainingMs / 1000); // Convertir a segundos
        } catch (Exception e) {
            return 0;
        }
    }

    //MÉTODOCENTRAL (PRO)
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}