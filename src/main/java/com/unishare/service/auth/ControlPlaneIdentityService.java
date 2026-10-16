
package com.unishare.service.auth;

import com.unishare.entity.rbac.GodAccount;
import com.unishare.entity.rbac.GodAccountRole;
import com.unishare.entity.rbac.GodRole;
import com.unishare.entity.rbac.GodRolePermission;
import com.unishare.repository.rbac.GodAccountRepository;
import com.unishare.repository.rbac.GodAccountRoleRepository;
import com.unishare.repository.rbac.GodRolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ControlPlaneIdentityService {

    private final GodAccountRepository accountRepository;
    private final GodAccountRoleRepository accountRoleRepository;
    private final GodRolePermissionRepository rolePermissionRepository;

    @Transactional(readOnly = true)
    public Set<GrantedAuthority> loadAuthorities(UUID accountId) {
        GodAccount account = accountRepository.findById(accountId)
                .filter(GodAccount::isActive)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Control-plane account unavailable"
                        )
                );

        List<GodAccountRole> assignments =
                accountRoleRepository.findByGodAccount_Id(accountId);

        if (assignments.isEmpty()) {
            throw new UsernameNotFoundException(
                    "Control-plane account has no assigned roles"
            );
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        for (GodAccountRole assignment : assignments) {
            GodRole role = assignment.getGodRole();

            if (role == null || role.getName() == null
                    || role.getName().isBlank()) {
                continue;
            }

            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.getName()
                    )
            );

            List<GodRolePermission> grants =
                    rolePermissionRepository.findByGodRole(role);

            for (GodRolePermission grant : grants) {
                if (grant.getPermission() == null
                        || grant.getPermission().getName() == null
                        || grant.getPermission().getName().isBlank()) {
                    continue;
                }

                authorities.add(
                        new SimpleGrantedAuthority(
                                grant.getPermission().getName()
                        )
                );
            }
        }

        if (authorities.isEmpty()) {
            throw new UsernameNotFoundException(
                    "Control-plane account has no usable authorities"
            );
        }

        return authorities;
    }

    @Transactional(readOnly = true)
    public List<String> loadRoleNames(UUID accountId) {
        return accountRoleRepository.findByGodAccount_Id(accountId)
                .stream()
                .map(GodAccountRole::getGodRole)
                .filter(role -> role != null && role.getName() != null)
                .map(GodRole::getName)
                .distinct()
                .toList();
    }
}
