package com.unishare.entity.auth;

import com.unishare.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken(
            String tokenHash,
            User user,
            LocalDateTime expiresAt
    ) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }
}