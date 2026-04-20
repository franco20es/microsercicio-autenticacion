package com.example.AUHT_SERVICE.SERVICE;

import com.example.AUHT_SERVICE.DTO.Request.GoogleLoginRequest;
import com.example.AUHT_SERVICE.DTO.Response.GoogleLoginResponse;

public interface GoogleAuthService {

    // Metodo para iniciar seccion con google
    GoogleLoginResponse loginWithGoogle(GoogleLoginRequest request);

    // Metodo para iniciar seccion con google
    GoogleLoginResponse registerWithGoogle(GoogleLoginRequest request);
}
