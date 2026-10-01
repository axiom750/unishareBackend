package com.unishare.service.usermanagement;

import com.unishare.entity.auth.PasswordResetToken;
import com.unishare.entity.user.User;
import com.unishare.repository.password.PasswordResetTokenRepository;
import com.unishare.repository.user.UserRepository;
import com.unishare.service.mail.MailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

        Optional<User> optionalUser = userRepository.findByEmail(email);

        // Don't reveal whether the account exists
        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        // Invalidate previous reset tokens
        tokenRepository.deleteByUser(user);

        // Generate raw token
        String rawToken = generateToken();

        // Store only the hash
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        tokenHash,
                        user,
                        LocalDateTime.now().plusMinutes(5)
                );

        tokenRepository.save(resetToken);

        // Send raw token through email
        mailService.sendPasswordResetEmail(
                user.getEmail(),
                rawToken
        );
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                tokenRepository
                        .findByTokenHashAndUsedFalse(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired password reset token"
                                )
                        );

        // Check expiry
        if (resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Invalid or expired password reset token"
            );
        }

        User user = resetToken.getUser();

        // Update password
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        // Make token single-use
        resetToken.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(resetToken);
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