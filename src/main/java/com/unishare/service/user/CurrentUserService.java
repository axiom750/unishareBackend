package com.unishare.service.user;

import com.unishare.dto.response.user.UserResponse;
import com.unishare.entity.user.User;
import com.unishare.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Resolves the currently authenticated user.
     *
     * The JWT filter sets {@code authentication.getName()} to the
     * numeric user ID (as a string). This method parses it and
     * loads the user with profile eagerly.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String identity = authentication.getName();

        if (identity == null || identity.isBlank()) {
            return null;
        }

        Long userId;
        try {
            userId = Long.parseLong(identity);
        } catch (NumberFormatException e) {
            return null;
        }

        User user = userService.findById(userId).orElse(null);

        if (user == null) {
            return null;
        }

        return userMapper.toResponse(user);
    }
}