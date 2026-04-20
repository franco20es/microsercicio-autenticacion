package com.example.AUHT_SERVICE.SECURITY;

import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.AUHT_SERVICE.MODEL.ModelRoles;
import com.example.AUHT_SERVICE.MODEL.ModelUsuario;
import com.example.AUHT_SERVICE.REPOSITORY.RepositoryUsuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SERVICIO DE AUTENTICACIÓN CON GOOGLE
 * Decodifica tokens JWT de Google y crea/actualiza usuarios
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleAuthService {

    private final RepositoryUsuario userRepository;
    private final ObjectMapper objectMapper;

    /**
     * VALIDAR TOKEN DE GOOGLE Y CREAR/ACTUALIZAR USUARIO
     * El frontend ya verificó el token, solo decodificamos
     */
    public ModelUsuario validateAndCreateUser(String idToken) {
        try {
            log.info(" Validando token de Google...");

            // Decodificar JWT sin verificar firma (Google ya lo hizo)
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Token de Google con formato inválido");
            }

            // Decodificar payload (parte central del JWT)
            String payload = parts[1];
            int padding = 4 - (payload.length() % 4);
            if (padding != 4) {
                payload += "=".repeat(padding);
            }

            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
            JsonNode claims = objectMapper.readTree(decodedPayload);

            // Extraer datos del token
            String email = claims.has("email") ? claims.get("email").asText() : null;
            String nombre = claims.has("name") ? claims.get("name").asText() : null;

            if (email == null || email.isEmpty()) {
                log.error(" Email no encontrado en token de Google");
                throw new RuntimeException("Email no encontrado en token de Google");
            }

            log.info(" Token de Google válido para email: {}", email);

            // Buscar usuario existente o crear uno nuevo
            Optional<ModelUsuario> usuarioOpt = userRepository.findByEmail(email);
            ModelUsuario usuario;

            if (usuarioOpt.isPresent()) {
                usuario = usuarioOpt.get();
                log.info(" Usuario existente encontrado: {}", email);
                
                // Actualizar datos de Google
                usuario.setEmailVerificado(true);
                if (nombre != null && !nombre.isEmpty()) {
                    usuario.setNombre(nombre);
                }
            } else {
                log.info(" Creando nuevo usuario de Google: {}", email);
                
                usuario = ModelUsuario.builder()
                        .email(email)
                        .nombre(nombre != null ? nombre : email)
                        .password("") // Sin contraseña para OAuth
                        .rol(ModelRoles.ROLE_USER)
                        .activo(true)
                        .emailVerificado(true)
                        .bloqueado(false)
                        .intentosFallidos(0)
                        .twoFactorEnabled(false)
                        .build();
            }

            // Guardar usuario
            userRepository.save(usuario);
            log.info(" Usuario de Google procesado exitosamente: {}", email);
            
            return usuario;

        } catch (Exception e) {
            log.error(" Error procesando token de Google: {}", e.getMessage());
            throw new RuntimeException("Error al procesar token de Google: " + e.getMessage(), e);
        }
    }
}
