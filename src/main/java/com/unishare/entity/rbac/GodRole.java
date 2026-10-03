package com.unishare.entity.rbac;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Control-plane role (e.g. SUPERUSER), held only by a {@link GodAccount}
 * through {@link GodAccountRole}.
 *
 * Deliberately unrelated to the normal RBAC {@link Role}: it is stored in its
 * own table, is never referenced by user_roles, and never takes part in
 * role_permissions.
 *
 * Lifecycle is owned exclusively by SecurityDeclarationSynchronizer; identity
 * and metadata come from @GodRole on UnishareApplication.
 */
@Entity
@Table(name = "god_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GodRole {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
