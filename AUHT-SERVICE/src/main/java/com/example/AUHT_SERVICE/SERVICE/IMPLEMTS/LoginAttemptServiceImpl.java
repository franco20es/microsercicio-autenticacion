package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import com.example.AUHT_SERVICE.MODEL.ModelUsuario;
import com.example.AUHT_SERVICE.REPOSITORY.RepositoryUsuario;
import com.example.AUHT_SERVICE.SERVICE.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final RepositoryUsuario userRepository;

    // Maximum number of failed login attempts before account locks
    private static final int MAX_INTENTOS = 5;

    // Lock duration in minutes
    private static final int TIEMPO_BLOQUEO_MINUTOS = 15;

    @Override
    // Register a failed login attempt for a specific account
    public void registrarIntentoFallido(String email) {
        ModelUsuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Get current failed attempts count
        int intentosActuales = usuario.getIntentosFallidos() != null ? 
            usuario.getIntentosFallidos() : 0;
        
        // Increment the counter
        usuario.setIntentosFallidos(intentosActuales + 1);
        
        log.warn("INTENTO FALLIDO: Failed login attempt for: {}. Attempts: {}/{}", 
            email, intentosActuales + 1, MAX_INTENTOS);

        // If max attempts reached, lock account automatically
        if ((intentosActuales + 1) >= MAX_INTENTOS) {
            usuario.setBloqueado(true);
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(TIEMPO_BLOQUEO_MINUTOS));
            
            log.error("BLOQUEADA: ACCOUNT LOCKED - max attempts reached: {}", email);
        }

        // Save changes to database
        userRepository.save(usuario);
    }

    @Override
    // Verify if an account is locked due to failed attempts
    public boolean estaCuentaBloqueada(String email) {
        ModelUsuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // If account is not locked, return false
        if (!usuario.isBloqueado()) {
            return false;
        }

        // If locked, check if lock time has expired
        LocalDateTime bloqueadoHasta = usuario.getBloqueadoHasta();
        
        if (bloqueadoHasta != null) {
            // If current time is after unlock time, unlock automatically
            if (LocalDateTime.now().isAfter(bloqueadoHasta)) {
                log.info("DESBLOQUEANDO: Auto-unlocking account - lock time expired: {}", email);
                desbloquearCuenta(email);
                return false;
            }
        }

        // Account is still locked
        log.warn("BLOQUEADA: Account locked: {}", email);
        return true;
    }

    @Override
    // Unlock account manually (only by support/admin)
    public void desbloquearCuenta(String email) {
        ModelUsuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Unlock the account
        usuario.setBloqueado(false);
        usuario.setBloqueadoHasta(null);
        
        // Clear failed attempts counter
        usuario.setIntentosFallidos(0);
        
        // Save changes
        userRepository.save(usuario);

        log.info("DESBLOQUEADA: Account unlocked manually by support: {}", email);
    }
}
