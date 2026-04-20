package com.example.AUHT_SERVICE.SERVICE;

import com.example.AUHT_SERVICE.DTO.Request.OtpRequest;
import com.example.AUHT_SERVICE.DTO.Response.AuthResponse;

public interface OtpService {

    void generateAndSendOtp(String email);
    
    // Cambiar de boolean a AuthResponse para retornar el token
    AuthResponse verifyOtp(OtpRequest request);
}
