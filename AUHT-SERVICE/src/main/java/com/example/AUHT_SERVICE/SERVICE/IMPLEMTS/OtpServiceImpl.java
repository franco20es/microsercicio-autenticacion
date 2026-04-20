package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.example.AUHT_SERVICE.DTO.Request.OtpRequest;
import com.example.AUHT_SERVICE.DTO.Response.AuthResponse;
import com.example.AUHT_SERVICE.MODEL.ModelUsuario;
import com.example.AUHT_SERVICE.REPOSITORY.RepositoryUsuario;
import com.example.AUHT_SERVICE.SERVICE.EmailService;
import com.example.AUHT_SERVICE.SERVICE.OtpService;
import com.example.AUHT_SERVICE.SERVICE.RedisService;
import com.example.AUHT_SERVICE.UTILS.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final RepositoryUsuario userRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    @Override
    public void generateAndSendOtp(String email) {
        ModelUsuario user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Generar OTP de 6 dígitos
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 🔴 GUARDAR EN REDIS CON EXPIRACIÓN AUTOMÁTICA (15 minutos)
        redisService.setOTP(email, code);
        log.info("✅ OTP guardado en Redis para: {} (expira en 15 min)", email);

        // URL del logo
        String logoUrl = "https://res.cloudinary.com/dgrdonnsk/image/upload/v1773947202/imagen02_r0mqdt.webp";

        String htmlContent = """
        <div style="background-color: #f6f9fc; padding: 40px 0; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
            <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 450px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e1e8ed; overflow: hidden;">
                <tr>
                    <td style="padding: 40px; text-align: center;">
                        <img src="%s" alt="Logo" width="100" style="margin-bottom: 25px;">
                        
                        <span style="color: #FF007F; font-weight: 800; font-size: 11px; text-transform: uppercase; letter-spacing: 3px; display: block; margin-bottom: 15px;">Seguridad</span>
                        
                        <h2 style="color: #1a202c; font-size: 22px; font-weight: 700; margin: 0 0 15px 0;">Verifica tu identidad</h2>
                        
                        <p style="color: #718096; font-size: 15px; line-height: 1.6; margin-bottom: 25px;">
                            Usa el siguiente código de seguridad para acceder al sistema. Este código es válido por <b>15 minutos</b>.
                        </p>

                        <div style="background-color: #f8fafc; border: 2px dashed #FF007F; padding: 20px; border-radius: 8px; display: inline-block; width: 80%%;">
                            <span style="font-family: 'Courier New', monospace; font-size: 32px; font-weight: 700; color: #1e293b; letter-spacing: 8px;">
                                %s
                            </span>
                        </div>

                        <p style="color: #a0aec0; font-size: 12px; margin-top: 30px;">
                            Si no solicitaste este código, por favor ignora este mensaje o contacta a soporte.
                        </p>
                    </td>
                </tr>
                <tr>
                    <td style="background-color: #fcfdfe; padding: 20px; text-align: center; border-top: 1px solid #edf2f7;">
                        <p style="margin: 0; color: #718096; font-size: 11px;">
                            &copy; 2026 Botica La Luz &bull; Ica, Perú
                        </p>
                    </td>
                </tr>
            </table>
        </div>
        """.formatted(logoUrl, code);

        log.info(" Enviando OTP a: {}", email);

        // Enviar por email
        emailService.sendEmail(
                email,
                "Tu código de seguridad: " + code,
                htmlContent
        );
    }

    @Override
    public AuthResponse verifyOtp(OtpRequest request) {

        log.info("Validando OTP para: {}", request.getEmail());

        ModelUsuario user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado: {}", request.getEmail());
                    return new RuntimeException("Usuario no encontrado");
                });

        //  VALIDAR OTP DESDE REDIS
        String codigoRecibido = request.getCodigo().trim();
        boolean isValidOTP = redisService.validateOTP(request.getEmail(), codigoRecibido);

        if (!isValidOTP) {
            log.warn(" OTP inválido o expirado para: {}", request.getEmail());
            throw new RuntimeException("OTP inválido o expirado");
        }

        log.info(" OTP válido para: {}", request.getEmail());

        // Generar token JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getRol().name());

        // Actualizar último login
        user.setUltimoLogin(LocalDateTime.now());
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken("")
                .email(user.getEmail())
                .rol(user.getRol().name())
                .twoFactorRequired(false)
                .build();
    }
}