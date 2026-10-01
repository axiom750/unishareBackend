package com.unishare.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${frontend.url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(
            String email,
            String token
    ) {

        log.info("[MAIL] Preparing password reset email for domain: {}", 
                email.substring(email.indexOf("@")));

        String resetLink =
                frontendUrl
                        + "/reset-password?token="
                        + token;

        log.debug("[MAIL] Password reset link generated with frontend URL");

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);
        message.setSubject(
                "UniShare - Password Reset"
        );

        message.setText(
                """
                Hello,

                We received a request to reset your UniShare password.

                Click the link below to reset your password:

                %s

                This link will expire in 5 minutes.

                If you did not request a password reset,
                you can safely ignore this email.

                Regards,
                UniShare Team
                """.formatted(resetLink)
        );

        try {
            mailSender.send(message);
            log.info("[MAIL] Password reset email sent successfully");
        } catch (Exception e) {
            log.error("[MAIL] Failed to send password reset email: {}", e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}