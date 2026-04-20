package com.example.AUHT_SERVICE.SERVICE;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Servicio centralizado para operaciones con Redis
 * Simplifica el uso de RedisTemplate en toda la aplicación
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== OPERACIONES BÁSICAS ====================

    /**
     * Guardar un valor en Redis con expiración
     */
    public void set(String key, Object value, long timeoutSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeoutSeconds));
            log.debug(" Redis SET: {} (expira en {}s)", key, timeoutSeconds);
        } catch (Exception e) {
            log.error(" Error en Redis SET: {}", key, e);
        }
    }

    /**
     * Guardar un valor en Redis sin expiración
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug(" Redis SET: {}", key);
        } catch (Exception e) {
            log.error(" Error en Redis SET: {}", key, e);
        }
    }

    /**
     * Obtener un valor de Redis
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug(" Redis GET: {}", key);
            return value;
        } catch (Exception e) {
            log.error(" Error en Redis GET: {}", key, e);
            return null;
        }
    }

    /**
     * Obtener como String
     */
    public String getString(String key) {
        return (String) get(key);
    }

    /**
     * Obtener como Integer (con conversión segura)
     */
    public Integer getInteger(String key) {
        try {
            Object value = get(key);
            if (value == null) return null;
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Long) return ((Long) value).intValue();
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.error(" Error al obtener Integer: {}", key, e);
            return null;
        }
    }

    /**
     * Verificar si una clave existe
     */
    public boolean hasKey(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            log.debug(" Redis HASKEY: {} -> {}", key, exists);
            return exists != null && exists;
        } catch (Exception e) {
            log.error(" Error en Redis HASKEY: {}", key, e);
            return false;
        }
    }

    /**
     * Eliminar una clave
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis DELETE: {}", key);
        } catch (Exception e) {
            log.error(" Error en Redis DELETE: {}", key, e);
        }
    }

    /**
     * Incrementar un valor numérico
     */
    public Long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            log.debug(" Redis INCREMENT: {} -> {}", key, value);
            return value;
        } catch (Exception e) {
            log.error(" Error en Redis INCREMENT: {}", key, e);
            return 0L;
        }
    }

    /**
     * Obtener tiempo de expiración (en segundos)
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("❌ Error en Redis GETEXPIRE: {}", key, e);
            return -1L;
        }
    }

    /**
     * Establecer expiración a una clave existente
     */
    public void expire(String key, long seconds) {
        try {
            redisTemplate.expire(key, Duration.ofSeconds(seconds));
            log.debug(" Redis EXPIRE: {} ({}s)", key, seconds);
        } catch (Exception e) {
            log.error(" Error en Redis EXPIRE: {}", key, e);
        }
    }

    // ==================== OPERACIONES ESPECÍFICAS PARA AUTH ====================

    /**
     * Cachear usuario después de login
     */
    public void cacheUser(Long userId, Object userData, long durationSeconds) {
        set("user:" + userId, userData, durationSeconds);
    }

    /**
     * Obtener usuario del caché
     */
    public Object getCachedUser(Long userId) {
        return get("user:" + userId);
    }

    /**
     * Registro de intento fallido de login
     */
    public void recordFailedLogin(String email) {
        String key = "failed_login:" + email;
        Integer attempts = getInteger(key);
        
        if (attempts == null) {
            set(key, 1L, 900); // 15 minutos
        } else {
            set(key, (long) (attempts + 1), 900);
        }
    }

    /**
     * Obtener intentos fallidos
     */
    public long getFailedLoginAttempts(String email) {
        Integer value = getInteger("failed_login:" + email);
        return value != null ? value.longValue() : 0L;
    }

    /**
     * Verificar si cuenta está bloqueada
     */
    public boolean isAccountLocked(String email) {
        long attempts = getFailedLoginAttempts(email);
        return attempts >= 5; // 5 intentos fallidos
    }

    /**
     * Limpiar intentos fallidos después de login exitoso
     */
    public void clearFailedLoginAttempts(String email) {
        delete("failed_login:" + email);
    }

    /**
     * Guardar OTP con expiración de 15 minutos
     */
    public void setOTP(String email, String code) {
        set("otp:" + email, code, 900); // 15 minutos
    }

    /**
     * Validar OTP
     */
    public boolean validateOTP(String email, String code) {
        String storedCode = getString("otp:" + email);
        
        if (storedCode == null) {
            return false; // OTP expirado
        }
        
        boolean isValid = storedCode.equals(code);
        
        if (isValid) {
            delete("otp:" + email); // Eliminar después de validar
        }
        
        return isValid;
    }

    /**
     * Guardar token de reset de contraseña (1 hora)
     */
    public void setPasswordResetToken(String token, String email) {
        set("password_reset:" + token, email, 3600); // 1 hora
    }

    /**
     * Obtener email del token de reset
     */
    public String getPasswordResetEmail(String token) {
        return getString("password_reset:" + token);
    }

    /**
     * Eliminar token de reset
     */
    public void deletePasswordResetToken(String token) {
        delete("password_reset:" + token);
    }

    /**
     * Añadir token a blacklist (logout)
     */
    public void blacklistToken(String token, long expirySeconds) {
        set("blacklist:" + token, "true", expirySeconds);
    }

    /**
     * Verificar si token está en blacklist
     */
    public boolean isTokenBlacklisted(String token) {
        return hasKey("blacklist:" + token);
    }
}

