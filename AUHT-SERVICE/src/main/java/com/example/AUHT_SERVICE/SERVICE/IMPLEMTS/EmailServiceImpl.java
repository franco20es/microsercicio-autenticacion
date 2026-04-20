package com.example.AUHT_SERVICE.SERVICE.IMPLEMTS;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.AUHT_SERVICE.SERVICE.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    //  Enviar email
    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            // 1. Crear un mensaje MIME en lugar de SimpleMailMessage
            MimeMessage message = mailSender.createMimeMessage();

            // 2. Usar MimeMessageHelper para configurar el contenido
            // El parámetro "true" indica que es un mensaje multipart (permite HTML)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            // 3. El segundo parámetro "true" en setText es CRUCIAL: activa el renderizado HTML
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Email HTML enviado exitosamente a: {}", to);

        } catch (Exception e) {
            log.error("Error al enviar email HTML: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar email: " + e.getMessage(), e);
        }
    }
}