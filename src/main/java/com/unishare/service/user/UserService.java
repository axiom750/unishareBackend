package com.unishare.service.user;

import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import com.unishare.repository.user.UserProfileRepository;
import com.unishare.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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

        Optional<User> existingUser = userRepository.findByGoogleId(googleId);

        if (existingUser.isPresent()) {

            User user = existingUser.get();

            ensureProfileExists(
                    user,
                    profilePictureUrl
            );

            return user;
        }

        existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {

            User user = existingUser.get();

            // Link Google account to existing user
            if (user.getGoogleId() == null) {

                user.setGoogleId(googleId);

                user = userRepository.save(user);
            }

            ensureProfileExists(
                    user,
                    profilePictureUrl
            );

            return user;
        }

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

        UserProfile profile = UserProfile.builder()
                        .user(savedUser)
                        .profilePictureUrl(profilePictureUrl)
                        .build();


        userProfileRepository.save(profile);


        return savedUser;
    }

    private void ensureProfileExists(
            User user,
            String profilePictureUrl) {

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


        // Profile doesn't exist → create it

        UserProfile profile =
                UserProfile.builder()
                        .user(user)
                        .profilePictureUrl(profilePictureUrl)
                        .build();

        userProfileRepository.save(profile);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}