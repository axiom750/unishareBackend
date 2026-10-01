package com.unishare.service.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

        String resetLink =
                frontendUrl
                        + "/reset-password?token="
                        + token;

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

        mailSender.send(message);
    }
}