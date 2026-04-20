package com.example.AUHT_SERVICE.SERVICE;

import com.example.AUHT_SERVICE.DTO.Request.CambiarPasswordRequest;
import com.example.AUHT_SERVICE.DTO.Request.RecuperarPasswordRequest;
import com.example.AUHT_SERVICE.DTO.Response.MessageResponse;

public interface PasswordService {

    MessageResponse recuperarPassword(RecuperarPasswordRequest request);
    MessageResponse changePassword(CambiarPasswordRequest request);
}
