package com.unishare.security;

import com.unishare.entity.rbac.Role;
import com.unishare.entity.user.User;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService that loads user authorities from the RBAC model.
 * 
 * Flow: User → Set<Role> → Set<Permission> → Spring Security Authorities
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userService.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + email));

        // Convert user's roles to Spring Security authorities
        Collection<? extends GrantedAuthority> authorities = buildAuthorities(user);

        log.debug("[SECURITY] Loaded user {} with {} authorities", 
                user.getEmail(), authorities.size());

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(String.valueOf(user.getId())) // Use ID as principal
                .password(user.getPassword())
                .authorities(authorities)  // Use authorities from RBAC
                .disabled(!user.isActive())
                .build();
    }

    /**
     * Build Spring Security authorities from user's roles.
     * Currently uses role names as authorities.
     * 
     * Future enhancement: Can be extended to include individual permissions
     * from Role.permissions for fine-grained authorization.
     *
     * @param user The user entity
     * @return Collection of granted authorities
     */
    private Collection<? extends GrantedAuthority> buildAuthorities(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            log.warn("[SECURITY] User {} has no roles assigned", user.getEmail());
            return java.util.Collections.emptyList();
        }

        // Convert roles to authorities with ROLE_ prefix (Spring Security convention)
        return user.getRoles().stream()
                .map(Role::getName)
                .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                .collect(Collectors.toList());
        
        // Future enhancement: Include permissions
        // Can add: role.getPermissions().stream().map(permission -> ...)
    }
}