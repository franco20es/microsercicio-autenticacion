package com.example.AUHT_SERVICE.SERVICE;

import com.example.AUHT_SERVICE.DTO.Request.TwoFactorRequest;
import com.example.AUHT_SERVICE.DTO.Response.AuthResponse;
import com.example.AUHT_SERVICE.DTO.Response.QrResponse;

public interface TwoFactorService {

    AuthResponse verify2FA(TwoFactorRequest request);
    QrResponse generateQr(String email);
}
