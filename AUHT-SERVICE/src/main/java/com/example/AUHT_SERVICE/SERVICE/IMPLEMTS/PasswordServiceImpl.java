package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import com.example.AUHT_SERVICE.DTO.Request.CambiarPasswordRequest;
import com.example.AUHT_SERVICE.DTO.Request.RecuperarPasswordRequest;
import com.example.AUHT_SERVICE.DTO.Response.MessageResponse;
import com.example.AUHT_SERVICE.MODEL.ModelUsuario;
import com.example.AUHT_SERVICE.REPOSITORY.RepositoryUsuario;
import com.example.AUHT_SERVICE.SECURITY.GoogleAuthService;
import com.example.AUHT_SERVICE.SERVICE.EmailService;
import com.example.AUHT_SERVICE.SERVICE.PasswordService;
import com.example.AUHT_SERVICE.UTILS.JwtUtil;
import com.example.AUHT_SERVICE.UTILS.OtpUtil;
import com.example.AUHT_SERVICE.UTILS.PasswordSeguroUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements PasswordService {

    private final RepositoryUsuario userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public MessageResponse recuperarPassword(RecuperarPasswordRequest request) {

        ModelUsuario user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Generar token de recuperación único
        String resetToken = UUID.randomUUID().toString();

        // Token válido por 1 hora
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(1);

        // Guardar token en el usuario
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(expiryTime);
        userRepository.save(user);

        // Enviar email con enlace de recuperación
        String resetLink = "http://localhost:4200/cambiar-password?token=" + resetToken; // Angular corre en puerto 4200
        String emailSubject = "Recuperación de Contraseña";
        String emailContent = "Haz clic en el siguiente enlace para recuperar tu contraseña:\n" +
                resetLink +
                "\n\nEste enlace expirará en 1 hora.";

        try {
            emailService.sendEmail(request.getEmail(), emailSubject, emailContent);
            return new MessageResponse("Se ha enviado un enlace de recuperación a tu email");
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el email de recuperación: " + e.getMessage());
        }
    }


    @Override
    public MessageResponse changePassword(CambiarPasswordRequest request) {

        // Buscar el usuario por token
        ModelUsuario user = userRepository.findAll()
                .stream()
                .filter(u -> u.getResetPasswordToken() != null &&
                        u.getResetPasswordToken().equals(request.getToken()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Token de recuperación inválido"));

        // Verificar que el token no haya expirado
        if (user.getResetPasswordTokenExpiry() == null ||
                user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token de recuperación expirado");
        }
        // Validar contraseña segura
        if(!PasswordSeguroUtil.validatePassword(request.getNewPassword())) {
            throw new RuntimeException(PasswordSeguroUtil.GetError());
        }

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        user.setFechaActualizacion(LocalDateTime.now());

        userRepository.save(user);

        return new MessageResponse("Contraseña actualizada correctamente");
    }


}
