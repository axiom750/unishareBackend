package com.unishare.service.user;

import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.repository.user.UserProfileRepository;
import com.unishare.repository.user.UserRepository;
import com.unishare.service.rbac.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public Optional<User> findByEmail(String email) {

        if (email == null) {
            return Optional.empty();
        }

        return userRepository.findByEmail(
                email.trim().toLowerCase()
        );
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    public Optional<User> findByGithubId(String githubId) {
        return userRepository.findByGithubId(githubId);
    }

    /**
     * Generates a unique UniShare username from an OAuth provider
     * username.
     */
    public String generateUniqueUsername(
            String baseUsername
    ) {

        String base =
                baseUsername == null
                        ? "user"
                        : baseUsername
                        .trim()
                        .replaceAll(
                                "[^a-zA-Z0-9_]",
                                "_"
                        );

        if (base.isBlank()) {
            base = "user";
        }

        /*
         * users.username has max length 50.
         */
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String candidate = base;

        int counter = 1;

        while (
                userRepository.existsByUsername(candidate)
        ) {

            String suffix =
                    "_" + counter;

            int maxBaseLength =
                    50 - suffix.length();

            String shortenedBase =
                    base.length() > maxBaseLength
                            ? base.substring(
                            0,
                            maxBaseLength
                    )
                            : base;

            candidate =
                    shortenedBase + suffix;

            counter++;
        }

        return candidate;
    }

    /**
     * Existing Google OAuth flow.
     *
     * New users obtain their roles from RoleService.
     * RoleService must resolve annotation-declared roles.
     */
    @Transactional
    public User processGoogleUser(
            String username,
            String email,
            String googleId,
            String profilePictureUrl
    ) {

        email =
                email.trim().toLowerCase();

        Optional<User> existingUser =
                userRepository.findByGoogleId(
                        googleId
                );

        if (existingUser.isPresent()) {

            User user =
                    existingUser.get();

            ensureProfileExists(
                    user,
                    profilePictureUrl
            );

            return user;
        }

        existingUser =
                userRepository.findByEmail(email);

        if (existingUser.isPresent()) {

            User user =
                    existingUser.get();

            if (user.getGoogleId() != null
                    && !user.getGoogleId().equals(googleId)) {

                throw new IllegalStateException(
                        "User account is already linked to another Google account"
                );
            }

            if (user.getGoogleId() == null) {

                user.setGoogleId(googleId);

                user =
                        userRepository.save(user);
            }

            ensureProfileExists(
                    user,
                    profilePictureUrl
            );

            return user;
        }

        User newUser =
                User.builder()
                        .email(email)
                        .username(
                                generateUniqueUsername(
                                        username
                                )
                        )
                        .googleId(googleId)
                        .authProvider(
                                com.unishare.enums.auth.AuthProvider.GOOGLE
                        )
                        .roles(
                                roleService
                                        .getDefaultUserRoles()
                        )
                        .active(true)
                        .password(
                                passwordEncoder.encode(
                                        UUID.randomUUID()
                                                .toString()
                                )
                        )
                        .build();

        User savedUser =
                userRepository.save(newUser);

        ensureProfileExists(
                savedUser,
                profilePictureUrl
        );

        return savedUser;
    }

    private void ensureProfileExists(
            User user,
            String profilePictureUrl
    ) {

        Optional<UserProfile> existingProfile =
                userProfileRepository.findByUser(user);

        if (existingProfile.isPresent()) {

            UserProfile profile =
                    existingProfile.get();

            if (profile.getProfilePictureUrl() == null
                    && profilePictureUrl != null) {

                profile.setProfilePictureUrl(
                        profilePictureUrl
                );

                userProfileRepository.save(profile);
            }

            return;
        }

        UserProfile profile =
                UserProfile.builder()
                        .user(user)
                        .profilePictureUrl(
                                profilePictureUrl
                        )
                        .build();

        userProfileRepository.save(profile);
    }

    public User save(User user) {

        return userRepository.save(user);
    }
}