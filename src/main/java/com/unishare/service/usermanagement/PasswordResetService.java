package com.unishare.service.usermanagement;

import com.unishare.entity.auth.PasswordResetToken;
import com.unishare.entity.user.User;
import com.unishare.repository.password.PasswordResetTokenRepository;
import com.unishare.repository.user.UserRepository;
import com.unishare.service.mail.MailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestPasswordReset(String email) {

        log.info("[AUTH] Password reset request received for email domain: {}", 
                email.substring(email.indexOf("@")));

        Optional<User> optionalUser = userRepository.findByEmail(email);

        // Don't reveal whether the account exists
        if (optionalUser.isEmpty()) {
            log.info("[AUTH] Password reset request completed (account not found - no action taken)");
            return;
        }

        User user = optionalUser.get();
        log.info("[AUTH] Password reset request - user found, proceeding with token generation");

        // Invalidate previous reset tokens
        tokenRepository.deleteByUser(user);
        log.debug("[AUTH] Previous password reset tokens invalidated for user");

        // Generate raw token
        String rawToken = generateToken();
        log.debug("[AUTH] Password reset token generated successfully");

        // Store only the hash
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        tokenHash,
                        user,
                        LocalDateTime.now().plusMinutes(5)
                );

        tokenRepository.save(resetToken);
        log.info("[AUTH] Password reset token stored with 5-minute expiration");

        // Send raw token through email
        mailService.sendPasswordResetEmail(
                user.getEmail(),
                rawToken
        );
        
        log.info("[AUTH] Password reset request completed successfully");
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {

        log.info("[AUTH] Password reset confirmation received");

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                tokenRepository
                        .findByTokenHashAndUsedFalse(tokenHash)
                        .orElseThrow(() -> {
                            log.warn("[AUTH] Password reset failed - invalid or already used token");
                            return new IllegalArgumentException(
                                    "Invalid or expired password reset token"
                            );
                        });

        // Check expiry
        if (resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {
            
            log.warn("[AUTH] Password reset failed - token expired");
            throw new IllegalArgumentException(
                    "Invalid or expired password reset token"
            );
        }

        log.debug("[AUTH] Password reset token validated successfully");

        User user = resetToken.getUser();

        // Update password
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );
        log.debug("[AUTH] User password updated with BCrypt hash");

        // Make token single-use
        resetToken.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(resetToken);
        
        log.info("[AUTH] Password reset completed successfully for user ID: {}", user.getId());
    }

    private String generateToken() {

        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    e
            );
        }
    }
}