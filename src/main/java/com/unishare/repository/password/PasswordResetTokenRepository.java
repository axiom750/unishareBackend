package com.unishare.repository.password;

import com.unishare.entity.auth.PasswordResetToken;
import com.unishare.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(
            String tokenHash
    );

    void deleteByUser(User user);
}