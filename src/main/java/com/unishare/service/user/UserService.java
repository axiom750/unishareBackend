package com.unishare.service.user;

import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import com.unishare.repository.user.UserProfileRepository;
import com.unishare.repository.user.UserRepository;
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

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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


    @Transactional
    public User processGoogleUser(
            String username,
            String email,
            String googleId,
            String profilePictureUrl) {

        log.info("[OAUTH] Processing Google OAuth user - Google ID: {}, email domain: {}", 
                googleId, getEmailDomain(email));

        Optional<User> existingUser = userRepository.findByGoogleId(googleId);

        if (existingUser.isPresent()) {
            log.debug("[OAUTH] Existing Google user found - ID: {}", existingUser.get().getId());
            User user = existingUser.get();

            ensureProfileExists(
                    user,
                    profilePictureUrl
            );

            return user;
        }

        existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            log.info("[OAUTH] Linking Google account to existing user - ID: {}", existingUser.get().getId());
            User user = existingUser.get();

            // Link Google account to existing user
            if (user.getGoogleId() == null) {

                user.setGoogleId(googleId);

                user = userRepository.save(user);
                log.debug("[OAUTH] Google account linked successfully");
            }

            ensureProfileExists(
                    user,
                    profilePictureUrl
            );

            return user;
        }

        log.info("[OAUTH] Creating new user from Google OAuth - email domain: {}", getEmailDomain(email));
        
        User newUser = User.builder()
                .email(email)
                .username(username)
                .googleId(googleId)
                .authProvider(AuthProvider.GOOGLE)
                .role(Roles.USER)
                .active(true)
                .password(
                        passwordEncoder.encode(
                                UUID.randomUUID().toString()
                        )
                )
                .build();


        User savedUser = userRepository.save(newUser);
        log.info("[OAUTH] New Google user created - ID: {}", savedUser.getId());

        UserProfile profile = UserProfile.builder()
                        .user(savedUser)
                        .profilePictureUrl(profilePictureUrl)
                        .build();


        userProfileRepository.save(profile);
        log.debug("[OAUTH] User profile created for user ID: {}", savedUser.getId());


        return savedUser;
    }

    private void ensureProfileExists(
            User user,
            String profilePictureUrl) {

        log.debug("[USER] Ensuring profile exists for user ID: {}", user.getId());

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
                log.debug("[USER] Profile picture updated for user ID: {}", user.getId());
            }

            return;
        }


        // Profile doesn't exist → create it
        log.debug("[USER] Creating new profile for user ID: {}", user.getId());

        UserProfile profile =
                UserProfile.builder()
                        .user(user)
                        .profilePictureUrl(profilePictureUrl)
                        .build();

        userProfileRepository.save(profile);
        log.debug("[USER] Profile created successfully for user ID: {}", user.getId());
    }

    public User save(User user) {
        log.debug("[USER] Saving user - ID: {}", user.getId() != null ? user.getId() : "new user");
        return userRepository.save(user);
    }
    
    /**
     * Extract email domain for logging (never log full email for privacy)
     */
    private String getEmailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return email.substring(email.indexOf("@"));
    }
}