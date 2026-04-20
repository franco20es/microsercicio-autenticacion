package com.example.AUHT_SERVICE.SERVICE;

import com.example.AUHT_SERVICE.DTO.Request.*;
import com.example.AUHT_SERVICE.DTO.Response.AuthResponse;
import com.example.AUHT_SERVICE.DTO.Response.MessageResponse;

public interface AuthService {

    // Metodo para validar el login
    AuthResponse login(LoginRequest request);

    // Metodo para registrar un nuevo usuario
    MessageResponse register(RegisterRequest request);

}


