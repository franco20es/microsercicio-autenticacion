package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import com.example.AUHT_SERVICE.DTO.Request.TwoFactorRequest;
import com.example.AUHT_SERVICE.DTO.Response.AuthResponse;
import com.example.AUHT_SERVICE.DTO.Response.QrResponse;
import com.example.AUHT_SERVICE.MODEL.ModelUsuario;
import com.example.AUHT_SERVICE.REPOSITORY.RepositoryUsuario;
import com.example.AUHT_SERVICE.SERVICE.TwoFactorService;
import com.example.AUHT_SERVICE.UTILS.JwtUtil;
import com.example.AUHT_SERVICE.UTILS.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorServiceImpl implements TwoFactorService {

    private final OtpUtil otpUtil;
    private final JwtUtil jwtUtil;
    private final RepositoryUsuario userRepository;

    @Override
    public AuthResponse verify2FA(TwoFactorRequest request) {

        ModelUsuario user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        log.info("═══════════════════════════════════════════════════════════════");
        log.info(" VERIFICANDO CÓDIGO 2FA");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info(" Email: {}", request.getEmail());
        log.info(" Secret recuperado de BD: {}", user.getSecret2FA());
        log.info(" Longitud del secret: {} caracteres", user.getSecret2FA() != null ? user.getSecret2FA().length() : "NULL");
        log.info(" Código ingresado por usuario: {}", request.getCodigo());
        log.info(" Longitud del código: {} caracteres", request.getCodigo().length());

        // OtpUtil.verifyCode lanzará excepciones específicas si algo falla
        otpUtil.verifyCode(user.getSecret2FA(), request.getCodigo());

        log.info("═══════════════════════════════════════════════════════════════");
        log.info(" CÓDIGO 2FA VÁLIDO - GENERANDO TOKEN JWT");
        log.info("═══════════════════════════════════════════════════════════════");

        //  METODO PARA GENERAR TOKEN CON ROL DESPUÉS DE VERIFICAR 2FA
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRol().name()
        );

        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken("")  // Generar o dejar vacío según tu lógica
                .email(user.getEmail())
                .rol(user.getRol().name())
                .twoFactorRequired(false)
                .build();
    }

    //  METODO PARA GENERAR QR DE GOOGLE AUTHENTICATOR, GUARDANDO EL SECRET EN EL USUARIO
    @Override
    public QrResponse generateQr(String email) {

        ModelUsuario user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String secret = otpUtil.generateSecret();

        log.info(" GENERANDO NUEVO QR PARA 2FA");
        log.info(" Email: {}", email);
        log.info(" Secret GENERADO: {}", secret);
        log.info(" Longitud del secret: {} caracteres", secret.length());

        // Guardar en BD
        user.setSecret2FA(secret);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        log.info(" Secret GUARDADO en BD");

        // Verificar que se guardó correctamente
        ModelUsuario usuarioVerificacion = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No se pudo verificar el usuario"));
        String secretEnBD = usuarioVerificacion.getSecret2FA();
        
        log.info("Secret RECUPERADO de BD: {}", secretEnBD);
        log.info(" ¿Son idénticos? {}", secret.equals(secretEnBD));
        
        if (!secret.equals(secretEnBD)) {
            log.error(" ¡ERROR! El secret cambió al guardarse!");
            log.error("  Generado: {}", secret);
            log.error("  En BD: {}", secretEnBD);
        }

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("INSTRUCCIONES PARA EL USUARIO:");
        log.info("1. Escanea el QR que aparece a continuación en Google Authenticator");
        log.info("2. Si ya tenías una cuenta con este email en GA, ELIMÍNALA primero");
        log.info("3. Inmediatamente después de escanear, copia el código que ve GA");
        log.info("4. Pega el código en el formulario DENTRO DE 30 SEGUNDOS");
        log.info("═══════════════════════════════════════════════════════════════");

        String qr = otpUtil.generateQrImage(email, secret);

        return QrResponse.builder()
                .qrImage(qr)
                .secret(secret)
                .build();
    }

}
