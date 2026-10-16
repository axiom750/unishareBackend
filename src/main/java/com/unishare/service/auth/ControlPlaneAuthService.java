
package com.unishare.service.auth;

import com.unishare.dto.request.rbac.ControlPlaneLoginRequest;
import com.unishare.dto.response.auth.ControlPlaneLoginResponse;
import com.unishare.entity.rbac.GodAccount;
import com.unishare.repository.rbac.GodAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ControlPlaneAuthService {

    private final GodAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ControlPlaneJwtService jwtService;
    private final ControlPlaneIdentityService identityService;

    @Transactional(readOnly = true)
    public ControlPlaneLoginResponse login(
            ControlPlaneLoginRequest request
    ) {
        String identifier = request.username().trim();
        String normalized = identifier.toLowerCase(Locale.ROOT);

        GodAccount account = accountRepository
                .findByUsername(identifier)
                .or(() -> accountRepository.findByEmail(normalized))
                .filter(GodAccount::isActive)
                .filter(candidate ->
                        candidate.getPassword() != null
                                && passwordEncoder.matches(
                                request.password(),
                                candidate.getPassword()
                        )
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid username/email or password"
                        )
                );

        List<String> roles = identityService.loadRoleNames(
                account.getId()
        );

        if (roles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Control-plane account has no assigned roles"
            );
        }

        String token = jwtService.generateToken(account.getId());

        return new ControlPlaneLoginResponse(
                token,
                "Bearer",
                jwtService.getExpiryMillis(),
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getDisplayName(),
                roles
        );
    }
}
