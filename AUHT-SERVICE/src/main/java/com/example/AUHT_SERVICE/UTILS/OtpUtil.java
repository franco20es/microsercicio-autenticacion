package com.example.AUHT_SERVICE.UTILS;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Component;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import com.example.AUHT_SERVICE.EXCEPTION.TwoFactorSecretNotConfiguredException;
import com.example.AUHT_SERVICE.EXCEPTION.TwoFactorCodeFormatException;
import com.example.AUHT_SERVICE.EXCEPTION.TwoFactorInvalidCodeException;

import java.util.Random;
import java.util.Base64;

@Component
public class OtpUtil {

    private final Random random;

    public OtpUtil() {
        this.random = new Random();
    }

    /**
     * Genera un secreto aleatorio en Base32 compatible 100% con Google Authenticator
     * Usando librería estándar JBoss AeroGear OTP
     */
    public String generateSecret() {
        byte[] randomBytes = new byte[20];
        random.nextBytes(randomBytes);
        String secret = Base32.encode(randomBytes);
        return secret;
    }

    /**
     * Verifica si el código TOTP es válido usando JBoss AeroGear
     * Lanza excepciones específicas para cada tipo de error
     */
    public boolean verifyCode(String secret, String code) {
        try {
            //  Validación 1: Secret no configurado
            if (secret == null || secret.isEmpty()) {
                throw new TwoFactorSecretNotConfiguredException(
                    "Debes configurar Google Authenticator primero. Escanea el código QR en la app."
                );
            }

            //  Validación 2: Formato de código inválido (debe ser exactamente 6 dígitos)
            if (code == null || !code.matches("\\d{6}")) {
                throw new TwoFactorCodeFormatException(
                    "El código 2FA debe tener exactamente 6 dígitos numéricos."
                );
            }

            Totp totp = new Totp(secret);
            String currentCode = totp.now();

            // JBoss AeroGear verifica automáticamente con tolerancia de ±1 ventana
            if (totp.verify(code)) {
                return true;
            }

            //  Código no válido
            throw new TwoFactorInvalidCodeException(
                "Código inválido o expirado. Intenta con el código actual de Google Authenticator."
            );

        } catch (TwoFactorSecretNotConfiguredException
                 | TwoFactorCodeFormatException
                 | TwoFactorInvalidCodeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println(" Error verificando código TOTP: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error interno verificando código TOTP: " + e.getMessage());
        }
    }

    /**
     * Genera un código OTP de 6 dígitos aleatorios para verificación por email
     */
    public String generarCodigoOtp() {
        int codigoOtp = 100000 + random.nextInt(900000);
        return String.valueOf(codigoOtp);
    }

    /**
     * Genera una imagen QR en formato Base64
     * El formato otpauth:// es estándar para Google Authenticator
     */
    public String generateQrImage(String email, String secret) {
        try {
            // Construir URL otpauth:// con todos los parámetros correctamente codificados
            String label = java.net.URLEncoder.encode(email, "UTF-8");
            
            String otpauthUrl = String.format(
                "otpauth://totp/%s?secret=%s&issuer=Hotel%%20System&algorithm=SHA1&digits=6&period=30",
                label,
                secret
            );

            // Generar QR con la URL correcta
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(otpauthUrl, BarcodeFormat.QR_CODE, 400, 400);

            // Convertir a PNG y luego a Base64 data URI
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUri = "data:image/png;base64," + base64Image;


            return dataUri;

        } catch (WriterException | java.io.IOException e) {
            throw new RuntimeException("Error generando imagen QR: " + e.getMessage());
        }
    }
}
