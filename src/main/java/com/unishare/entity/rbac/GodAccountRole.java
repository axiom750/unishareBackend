package com.unishare.entity.rbac;

import jakarta.persistence.*;
import lombok.*;

/**
 * GodAccount -> GodRole assignment (control plane only).
 *
 * References {@link GodRole}, never the normal RBAC {@link Role}.
 * Lifecycle is owned exclusively by SecurityDeclarationSynchronizer.
 */
@Entity
@Table(
        name = "god_account_roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_god_account_god_role",
                        columnNames = {
                                "god_account_id",
                                "god_role_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GodAccountRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "god_account_id",
            nullable = false
    )
    private GodAccount godAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "god_role_id",
            nullable = false
    )
    private GodRole godRole;
}
