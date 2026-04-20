package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.AUHT_SERVICE.SERVICE.TokenService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenServiceImpl implements TokenService {

    //  Clave secreta para firmar los tokens 
    @Value("${jwt.secret}")
    private String secret;

    //  Generar access token con email y rol, expiración corta (15 min)
    @Override
    public String generateAccessToken(String email, String rol) {
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        return Jwts.builder()
                .setSubject(email)
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 min
                .signWith(Keys.hmacShaKeyFor(decodedKey),
                        SignatureAlgorithm.HS512)
                .compact();
    }

    //  Generar refresh token con expiración más larga
    @Override
    public String generateRefreshToken(String email) {
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        return Jwts.builder()
                .setSubject(email)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // 7 días
                .signWith(Keys.hmacShaKeyFor(decodedKey),
                        SignatureAlgorithm.HS512)
                .compact();
    }

    //  Validar token verificando firma y expiración
    @Override
    public boolean validateToken(String token) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secret);
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(decodedKey))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //  Extraer email del token
    @Override
    public String extractEmail(String token) {
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(decodedKey))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
