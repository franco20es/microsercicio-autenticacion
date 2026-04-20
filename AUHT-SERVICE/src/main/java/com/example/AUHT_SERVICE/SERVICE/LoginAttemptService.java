package com.example.AUHT_SERVICE.SERVICE;

public interface LoginAttemptService {

    // Registrar un intento fallido de login
    void registrarIntentoFallido(String email);

    // Verificar si la cuenta está bloqueada
    boolean estaCuentaBloqueada(String email);

    // Desbloquear la cuenta manualmente (solo por soporte)
    void desbloquearCuenta(String email);

}


