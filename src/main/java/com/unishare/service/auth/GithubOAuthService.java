package com.unishare.service.auth;

import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.repository.user.UserProfileRepository;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GithubOAuthService {

    private final RestTemplate restTemplate;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Value("${github.clientId}")
    private String clientId;

    @Value("${github.clientSecret}")
    private String clientSecret;

    @Value("${github.redirectUrl}")
    private String redirectUrl;

    @Value("${github.tokenUrl}")
    private String tokenUrl;

    @Value("${github.userUrl}")
    private String userUrl;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${production.status}")
    private boolean isProduction;

    public ResponseEntity<Void> initiateLogin() {

        String githubAuthUrl =
                "https://github.com/login/oauth/authorize"
                        + "?client_id=" + clientId
                        + "&redirect_uri=" + redirectUrl
                        + "&scope=read:user%20user:email";

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, githubAuthUrl)
                .build();
    }

    public ResponseEntity<Void> handleCallback(String code) {

        try {

            String accessToken =
                    exchangeCodeForToken(code);

            Map<String, Object> userInfo =
                    fetchGithubUser(accessToken);

            String githubId =
                    String.valueOf(userInfo.get("id"));

            String email =
                    resolveEmail(userInfo, accessToken);

            String avatarUrl =
                    (String) userInfo.get("avatar_url");

            String username =
                    (String) userInfo.get("login");


            User user =
                    processGithubUser(
                            username,
                            email,
                            avatarUrl,
                            githubId
                    );


            String jwt =
                    jwtService.generateToken(
                            user.getId(),
                            user.getEmail(),
                            roleService.getPrimaryRoleName(user.getRoles())
                    );


            ResponseCookie cookie =
                    buildCookie(jwt);

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .header(
                            HttpHeaders.SET_COOKIE,
                            cookie.toString()
                    )
                    .header(
                            HttpHeaders.LOCATION,
                            frontendUrl
                    )
                    .build();

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .header(
                            HttpHeaders.LOCATION,
                            frontendUrl + "/login-error"
                    )
                    .build();
        }
    }

    private String exchangeCodeForToken(String code) {

        MultiValueMap<String, String> params =
                new LinkedMultiValueMap<>();

        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUrl);


        HttpHeaders headers =
                new HttpHeaders();

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON)
        );


        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);


        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        tokenUrl,
                        request,
                        Map.class
                );


        if (response.getBody() == null ||
                response.getBody().get("access_token") == null) {

            throw new RuntimeException(
                    "Failed to get GitHub access token"
            );
        }


        return (String)
                response.getBody().get("access_token");
    }


    private Map<String, Object> fetchGithubUser(
            String accessToken) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(accessToken);


        HttpEntity<Void> request =
                new HttpEntity<>(headers);


        ResponseEntity<Map> response =
                restTemplate.exchange(
                        userUrl,
                        HttpMethod.GET,
                        request,
                        Map.class
                );


        if (response.getBody() == null) {

            throw new RuntimeException(
                    "Failed to fetch GitHub user"
            );
        }


        return response.getBody();
    }

    private String resolveEmail(
            Map<String, Object> userInfo,
            String accessToken) {

        String email =
                (String) userInfo.get("email");

        if (email != null) {
            return email;
        }


        try {

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request =
                    new HttpEntity<>(headers);

            ResponseEntity<List> response =
                    restTemplate.exchange(
                            "https://api.github.com/user/emails",
                            HttpMethod.GET,
                            request,
                            List.class
                    );


            List<Map<String, Object>> emails =
                    response.getBody();


            if (emails != null) {

                for (Map<String, Object> entry : emails) {

                    if (Boolean.TRUE.equals(
                            entry.get("primary"))) {

                        String primaryEmail =
                                (String) entry.get("email");

                        if (primaryEmail != null) {
                            return primaryEmail;
                        }
                    }
                }
            }

        } catch (Exception ignored) {
            // Continue to fallback
        }


        return githubIdFallback(userInfo);
    }

    private String githubIdFallback(
            Map<String, Object> userInfo) {

        return userInfo.get("id")
                + "@github.local";
    }



    private User processGithubUser(
            String username,
            String email,
            String profilePictureUrl,
            String githubId) {


        Optional<User> existingUser =
                userService.findByGithubId(githubId);

        if (existingUser.isEmpty()) {

            existingUser =
                    userService.findByEmail(email);
        }

        if (existingUser.isPresent()) {

            User user =
                    existingUser.get();


            // Link GitHub account if necessary
            if (user.getGithubId() == null) {

                user.setGithubId(githubId);

                user =
                        userService.save(user);
            }


            // Make sure profile exists
            createProfileIfMissing(
                    user,
                    profilePictureUrl
            );


            return user;
        }

        User newUser = new User();

        newUser.setGithubId(githubId);
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setAuthProvider(AuthProvider.GITHUB);
        newUser.setActive(true);
        newUser.setRoles(roleService.getDefaultUserRoles()); // Assign default roles
        newUser.setPassword(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()
                )
        );


        User savedUser =
                userService.save(newUser);

        createProfileIfMissing(
                savedUser,
                profilePictureUrl
        );


        return savedUser;
    }

    private void createProfileIfMissing(
            User user,
            String profilePictureUrl) {

        Optional<UserProfile> existingProfile =
                userProfileRepository.findByUser(user);


        if (existingProfile.isPresent()) {

            UserProfile profile = existingProfile.get();


            if (profile.getProfilePictureUrl() == null &&
                    profilePictureUrl != null) {

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
                        .profilePictureUrl(profilePictureUrl)
                        .build();


        userProfileRepository.save(profile);
    }


    private ResponseCookie buildCookie(String jwt) {

        ResponseCookie.ResponseCookieBuilder cookieBuilder =
                ResponseCookie
                        .from("token", jwt)
                        .httpOnly(true)
                        .secure(isProduction)
                        .path("/")
                        .maxAge(60 * 60 * 24)
                        .sameSite(
                                isProduction
                                        ? "None"
                                        : "Lax"
                        );


        if (!isProduction) {
            cookieBuilder.domain("localhost");
        }


        return cookieBuilder.build();
    }
}