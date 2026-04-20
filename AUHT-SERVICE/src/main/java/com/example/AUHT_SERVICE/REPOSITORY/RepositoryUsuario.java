package com.example.AUHT_SERVICE.REPOSITORY;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.AUHT_SERVICE.MODEL.ModelUsuario;

public interface RepositoryUsuario  extends JpaRepository<ModelUsuario, Long> {
    
    //  Buscar por email (clave para login)
    Optional<ModelUsuario> findByEmail(String email);

    //  Verificar si existe email
    boolean existsByEmail(String email);

    //  Buscar por token de verificación (email)
    Optional<ModelUsuario> findByTokenVerificacion(String token);
    
}
