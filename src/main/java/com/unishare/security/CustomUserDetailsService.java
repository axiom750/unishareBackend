
package com.unishare.security;

import com.unishare.entity.rbac.Permission;
import com.unishare.entity.rbac.Role;
import com.unishare.entity.user.User;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException(
                    "Email must not be empty"
            );
        }

        User user = userService.findByEmail(email.trim())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );

        Collection<GrantedAuthority> authorities =
                buildAuthorities(user);

        log.debug(
                "[SECURITY] Loaded user ID {} with {} authorities",
                user.getId(),
                authorities.size()
        );

        return org.springframework.security.core.userdetails.User
                .withUsername(String.valueOf(user.getId()))
                .password(
                        user.getPassword() != null
                                ? user.getPassword()
                                : ""
                )
                .authorities(authorities)
                .disabled(!user.isActive())
                .build();
    }

    /**
     * Builds authorities from the user's RBAC roles and permissions.
     *
     * Role example: ROLE_USER
     * Permission example: RIDE_CREATE
     */
    private Collection<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            log.warn(
                    "[SECURITY] User ID {} has no roles assigned",
                    user.getId()
            );

            return authorities;
        }

        for (Role role : user.getRoles()) {
            if (role == null || role.getName() == null
                    || role.getName().isBlank()) {
                continue;
            }

            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.getName()
                    )
            );

            if (role.getPermissions() == null) {
                continue;
            }

            for (Permission permission : role.getPermissions()) {
                if (permission == null
                        || permission.getName() == null
                        || permission.getName().isBlank()) {
                    continue;
                }

                authorities.add(
                        new SimpleGrantedAuthority(
                                permission.getName()
                        )
                );
            }
        }

        return authorities;
    }
}
