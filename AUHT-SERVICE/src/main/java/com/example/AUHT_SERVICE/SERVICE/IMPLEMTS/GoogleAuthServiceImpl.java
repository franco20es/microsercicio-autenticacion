package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import com.example.AUHT_SERVICE.DTO.Request.GoogleLoginRequest;
import com.example.AUHT_SERVICE.DTO.Response.GoogleLoginResponse;
import com.example.AUHT_SERVICE.EXCEPTION.DuplicateEmailException;
import com.example.AUHT_SERVICE.MODEL.ModelUsuario;
import com.example.AUHT_SERVICE.REPOSITORY.RepositoryUsuario;
import com.example.AUHT_SERVICE.SERVICE.EmailService;
import com.example.AUHT_SERVICE.SERVICE.GoogleAuthService;
import com.example.AUHT_SERVICE.UTILS.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    // Inyectamos dependencias necesarias

    private final RepositoryUsuario userRepository;
    private final com.example.AUHT_SERVICE.SECURITY.GoogleAuthService googleValidator;
    private final OtpUtil otpUtil;
    private final EmailService emailService;

    @Override
    public GoogleLoginResponse loginWithGoogle(GoogleLoginRequest request) {
        log.info("Login con Google");

        // Validar token y crear/obtener usuario
        ModelUsuario user = googleValidator.validateAndCreateUser(request.getIdToken());

        // Generar OTP
        String codigoOtp = otpUtil.generarCodigoOtp();
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(15);

        user.setCodigoOtp(codigoOtp);
        user.setExpiracionOtp(expiracion);
        userRepository.save(user);

        log.info("OTP enviado a: {}", user.getEmail());

        // Enviar OTP con HTML y estilos
        try {
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
                                &copy; 2026 Gream Demo &bull; Ica, Perú
                            </p>
                        </td>
                    </tr>
                </table>
            </div>
            """.formatted(logoUrl, codigoOtp);

            emailService.sendEmail(
                user.getEmail(),
                "Tu código de seguridad: " + codigoOtp,
                htmlContent
            );
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        }

        return GoogleLoginResponse.builder()
                .email(user.getEmail())
                .nombre(user.getNombre())
                .rol(user.getRol().name())
                .proveedor("google")
                .twoFactorRequired(true)
                .build();
    }

    /**
     * REGISTRO CON GOOGLE
     * Valida que el email NO exista antes de registrar
     * IMPORTANTE: Diferente a login, que crea/actualiza automáticamente
     */
    @Override
    public GoogleLoginResponse registerWithGoogle(GoogleLoginRequest request) {
        log.info("🔐 Iniciando registro con Google");

        try {
            // 1. Decodificar y validar token de Google
            ModelUsuario user = googleValidator.validateAndCreateUser(request.getIdToken());
            String email = user.getEmail();

            log.debug("📧 Email extraído del token Google: {}", email);

            // ========== VALIDACIÓN CRÍTICA ==========
            // 2. VERIFICAR SI EMAIL YA EXISTE EN BD
            if (userRepository.existsByEmail(email)) {
                log.warn("⚠️ INTENTO DE REGISTRO DUPLICADO CON GOOGLE: {}", email);
                
                // Lanzar excepción personalizada
                throw new DuplicateEmailException(
                    "El email (" + email + ") ya está registrado. " +
                    "Por favor usa LOGIN con Google o intenta con otro email."
                );
            }

            log.info("✅ Email {} disponible para registro", email);

            // 3. Proceder con registro normal (generar OTP, enviar email)
            return proceedWithGoogleRegistration(user);

        } catch (DuplicateEmailException e) {
            // Este error será capturado por GlobalExceptionHandler y convertido a HTTP 400
            log.warn("❌ Error de email duplicado en registro Google: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            // Este error será capturado por GlobalExceptionHandler y convertido a HTTP 400
            log.warn("❌ Error de validación en registro Google: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado en registro Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando token de Google: " + e.getMessage());
        }
    }

    /**
     * PROCEDER CON REGISTRO GOOGLE
     * Se ejecuta después de validar que el email es único
     * Genera OTP y envía email
     */
    private GoogleLoginResponse proceedWithGoogleRegistration(ModelUsuario user) {
        log.info("📝 Registrando nuevo usuario Google: {}", user.getEmail());

        // Generar OTP
        String codigoOtp = otpUtil.generarCodigoOtp();
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(15);

        user.setCodigoOtp(codigoOtp);
        user.setExpiracionOtp(expiracion);
        userRepository.save(user);

        log.info("✅ Usuario guardado en BD: {}", user.getEmail());

        // Enviar OTP por email de forma asincrónica
        enviarOtpGoogleRegistro(user.getEmail(), codigoOtp);

        log.info("📧 OTP enviado a: {}", user.getEmail());

        return GoogleLoginResponse.builder()
                .email(user.getEmail())
                .nombre(user.getNombre())
                .rol(user.getRol().name())
                .proveedor("google")
                .twoFactorRequired(true)
                .build();
    }

    /**
     * ENVIAR OTP POR EMAIL PARA REGISTRO GOOGLE
     * Email con estilos HTML
     */
    private void enviarOtpGoogleRegistro(String email, String codigoOtp) {
        try {
            String logoUrl = "https://res.cloudinary.com/dgrdonnsk/image/upload/v1773947202/imagen02_r0mqdt.webp";

            String htmlContent = """
            <div style="background-color: #f6f9fc; padding: 40px 0; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 450px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e1e8ed; overflow: hidden;">
                    <tr>
                        <td style="padding: 40px; text-align: center;">
                            <img src="%s" alt="Logo" width="100" style="margin-bottom: 25px;">
                            
                            <span style="color: #FF007F; font-weight: 800; font-size: 11px; text-transform: uppercase; letter-spacing: 3px; display: block; margin-bottom: 15px;">Bienvenido</span>
                            
                            <h2 style="color: #1a202c; font-size: 22px; font-weight: 700; margin: 0 0 15px 0;">Verifica tu identidad</h2>
                            
                            <p style="color: #718096; font-size: 15px; line-height: 1.6; margin-bottom: 25px;">
                                Tu registro con Google está casi completo. Usa el siguiente código para verificar tu cuenta. Este código es válido por <b>15 minutos</b>.
                            </p>

                            <div style="background-color: #f8fafc; border: 2px dashed #FF007F; padding: 20px; border-radius: 8px; display: inline-block; width: 80%%;">
                                <span style="font-family: 'Courier New', monospace; font-size: 32px; font-weight: 700; color: #1e293b; letter-spacing: 8px;">
                                    %s
                                </span>
                            </div>

                            <p style="color: #a0aec0; font-size: 12px; margin-top: 30px;">
                                Si no realizaste este registro, por favor ignora este mensaje o contacta a soporte.
                            </p>
                        </td>
                    </tr>
                    <tr>
                        <td style="background-color: #fcfdfe; padding: 20px; text-align: center; border-top: 1px solid #edf2f7;">
                            <p style="margin: 0; color: #718096; font-size: 11px;">
                                &copy; 2026 Gream Demo &bull; Ica, Perú
                            </p>
                        </td>
                    </tr>
                </table>
            </div>
            """.formatted(logoUrl, codigoOtp);

            emailService.sendEmail(
                email,
                "Tu código de verificación: " + codigoOtp,
                htmlContent
            );

            log.debug("✅ Email OTP enviado correctamente a: {}", email);

        } catch (Exception e) {
            log.error("⚠️ Error enviando OTP a {}: {}", email, e.getMessage());
            // No lanzar excepción aquí - el email es "nice to have" pero el registro debe continuar
        }
    }
}
