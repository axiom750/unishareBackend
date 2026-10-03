package com.unishare.entity.rbac;

import jakarta.persistence.*;
import lombok.*;

/**
 * GodRole -> Permission grant (control plane only).
 *
 * Completely separate from role_permissions: references {@link GodRole},
 * never the normal RBAC {@link Role}. Only CONTROL_PLANE permissions are
 * granted here.
 *
 * Lifecycle is owned exclusively by SecurityDeclarationSynchronizer.
 */
@Entity
@Table(
        name = "god_role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_god_role_permission",
                        columnNames = {
                                "god_role_id",
                                "permission_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GodRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "god_role_id",
            nullable = false
    )
    private GodRole godRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "permission_id",
            nullable = false
    )
    private Permission permission;
}
