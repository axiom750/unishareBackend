package com.unishare.service.user;

import com.unishare.entity.user.User;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.enums.user.Roles;
import com.unishare.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    public Optional<User> findByGithubId(String githubId) {
        return userRepository.findByGithubId(githubId);
    }


    public User processGoogleUser(String username,String email, String googleId,String profilePictureURL) {

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                return userRepository.save(user);
            }

            return user;
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setGoogleId(googleId);
        newUser.setAuthProvider(AuthProvider.GOOGLE);
        newUser.setRole(Roles.USER);
        newUser.setActive(true);
        newUser.setUserProfilePictureURL(profilePictureURL);

        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        return userRepository.save(newUser);
    }


    public User save(User user) {
        return userRepository.save(user);
    }
}