package com.example.AUHT_SERVICE.DTO.Response;

import lombok.Builder;
import lombok.Data;
// DTO para la respuesta de generación de QR, que incluye la imagen en base64 y el secreto para 2FA.
@Data
@Builder
public class QrResponse {
    private String qrImage;   // base64
    private String secret;
}