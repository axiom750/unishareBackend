package com.unishare.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MailService {

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    private final RestClient restClient;

    public MailService(
            @Value("${brevo.api-key}") String apiKey,
            @Value("${mail.from-email}") String fromEmail,
            @Value("${mail.from-name:UniShare}") String fromName
    ) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    public void sendPasswordResetEmail(String email, String resetLink) {

        log.info(
                "[MAIL] Preparing password reset email for domain: {}",
                getDomain(email)
        );

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", fromName);
        sender.put("email", fromEmail);

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", email);

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("sender", sender);
        requestBody.put("to", List.of(recipient));
        requestBody.put("subject", "UniShare Password Reset");

        requestBody.put(
                "htmlContent",
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>UniShare Password Reset</title>
                </head>

                <body style="font-family: Arial, sans-serif; line-height: 1.6;">

                    <h2>Reset Your UniShare Password</h2>

                    <p>
                        We received a request to reset your UniShare password.
                    </p>

                    <p>
                        Click the button below to create a new password:
                    </p>

                    <p>
                        <a href="%s"
                           style="
                               display:inline-block;
                               padding:12px 20px;
                               background:#000000;
                               color:#ffffff;
                               text-decoration:none;
                               border-radius:6px;
                           ">
                            Reset Password
                        </a>
                    </p>

                    <p>
                        This password reset link will expire in
                        <strong>5 minutes</strong>.
                    </p>

                    <p>
                        If you did not request a password reset,
                        you can safely ignore this email.
                    </p>

                    <p>
                        Regards,<br>
                        UniShare Team
                    </p>

                </body>
                </html>
                """
                        .formatted(resetLink)
        );

        try {

            restClient
                    .post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "[MAIL] Password reset email sent successfully to domain: {}",
                    getDomain(email)
            );

        } catch (Exception exception) {

            log.error(
                    "[MAIL] Failed to send password reset email: {}",
                    exception.getMessage()
            );

            throw new RuntimeException(
                    "Unable to send password reset email",
                    exception
            );
        }
    }

    private String getDomain(String email) {

        if (email == null || !email.contains("@")) {
            return "unknown";
        }

        return email.substring(email.indexOf("@"));
    }
}