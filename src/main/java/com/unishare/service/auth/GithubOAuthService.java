package com.unishare.service.auth;

import com.unishare.entity.user.User;
import com.unishare.entity.user.UserProfile;
import com.unishare.enums.auth.AuthProvider;
import com.unishare.repository.user.UserProfileRepository;
import com.unishare.service.rbac.RoleService;
import com.unishare.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
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

    /**
     * Starts GitHub OAuth login.
     */
    public ResponseEntity<Void> initiateLogin() {

        String githubAuthUrl =
                UriComponentsBuilder
                        .fromUriString(
                                "https://github.com/login/oauth/authorize"
                        )
                        .queryParam("client_id", clientId)
                        .queryParam("redirect_uri", redirectUrl)
                        .queryParam(
                                "scope",
                                "read:user user:email"
                        )
                        .build()
                        .encode()
                        .toUriString();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(
                        HttpHeaders.LOCATION,
                        githubAuthUrl
                )
                .build();
    }

    /**
     * Handles GitHub OAuth callback.
     */
    public ResponseEntity<Void> handleCallback(String code) {

        try {

            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException(
                        "GitHub OAuth code is missing"
                );
            }

            /*
             * 1. Exchange authorization code for access token.
             */
            String accessToken =
                    exchangeCodeForToken(code);

            /*
             * 2. Fetch authenticated GitHub account.
             */
            Map<String, Object> githubUser =
                    fetchGithubUser(accessToken);

            String githubId =
                    extractRequiredString(
                            githubUser,
                            "id"
                    );

            String githubUsername =
                    extractRequiredString(
                            githubUser,
                            "login"
                    );

            String avatarUrl =
                    extractOptionalString(
                            githubUser,
                            "avatar_url"
                    );

            /*
             * 3. Get a verified GitHub email.
             */
            String email =
                    resolveVerifiedEmail(accessToken);

            /*
             * 4. Find/link/create UniShare account.
             */
            User user =
                    processGithubUser(
                            githubUsername,
                            email,
                            avatarUrl,
                            githubId
                    );

            /*
             * 5. Generate UniShare JWT.
             *
             * RoleService obtains the role from the runtime
             * role entity. It does not define role names.
             */
            String primaryRole =
                    roleService.getPrimaryRoleName(
                            user.getRoles()
                    );

            String jwt =
                    jwtService.generateToken(
                            user.getId(),
                            user.getEmail(),
                            primaryRole
                    );

            /*
             * 6. Store JWT in HttpOnly cookie.
             */
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

        } catch (Exception exception) {

            /*
             * Never expose OAuth/provider errors to the client.
             *
             * The authorization code and access token are never logged.
             */
            log.error(
                    "[OAUTH] GitHub authentication failed",
                    exception
            );

            return redirectToLoginError();
        }
    }

    /**
     * Exchanges GitHub authorization code for access token.
     */
    private String exchangeCodeForToken(String code) {

        MultiValueMap<String, String> params =
                new LinkedMultiValueMap<>();

        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUrl);

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON)
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(
                        params,
                        headers
                );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        tokenUrl,
                        request,
                        Map.class
                );

        Map body = response.getBody();

        if (body == null) {
            throw new IllegalStateException(
                    "GitHub token response is empty"
            );
        }

        Object accessToken =
                body.get("access_token");

        if (!(accessToken instanceof String token)
                || token.isBlank()) {

            throw new IllegalStateException(
                    "GitHub access token was not returned"
            );
        }

        return token;
    }

    /**
     * Fetches the authenticated GitHub user.
     */
    private Map<String, Object> fetchGithubUser(
            String accessToken
    ) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(accessToken);

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON)
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        userUrl,
                        HttpMethod.GET,
                        request,
                        Map.class
                );

        Map body = response.getBody();

        if (body == null) {
            throw new IllegalStateException(
                    "GitHub user response is empty"
            );
        }

        return body;
    }

    /**
     * Gets a verified GitHub email.
     *
     * GitHub's /user endpoint can return null for email,
     * therefore /user/emails is used.
     */
    private String resolveVerifiedEmail(
            String accessToken
    ) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(accessToken);

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON)
        );

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

        if (emails == null || emails.isEmpty()) {
            throw new IllegalStateException(
                    "GitHub account has no available email"
            );
        }

        /*
         * Prefer primary + verified email.
         */
        for (Map<String, Object> emailEntry : emails) {

            boolean primary =
                    Boolean.TRUE.equals(
                            emailEntry.get("primary")
                    );

            boolean verified =
                    Boolean.TRUE.equals(
                            emailEntry.get("verified")
                    );

            if (primary && verified) {

                String email =
                        extractOptionalString(
                                emailEntry,
                                "email"
                        );

                if (email != null) {
                    return normalizeEmail(email);
                }
            }
        }

        /*
         * Otherwise use any verified email.
         */
        for (Map<String, Object> emailEntry : emails) {

            boolean verified =
                    Boolean.TRUE.equals(
                            emailEntry.get("verified")
                    );

            if (!verified) {
                continue;
            }

            String email =
                    extractOptionalString(
                            emailEntry,
                            "email"
                    );

            if (email != null) {
                return normalizeEmail(email);
            }
        }

        throw new IllegalStateException(
                "GitHub account has no verified email"
        );
    }

    /**
     * Finds an existing GitHub user, safely links an existing
     * verified-email account, or creates a new normal user.
     */
    private User processGithubUser(
            String githubUsername,
            String email,
            String profilePictureUrl,
            String githubId
    ) {

        /*
         * STEP 1:
         *
         * GitHub ID is the primary provider identity.
         */
        Optional<User> existingByGithubId =
                userService.findByGithubId(githubId);

        if (existingByGithubId.isPresent()) {

            User user =
                    existingByGithubId.get();

            ensureProfileExists(
                    user,
                    profilePictureUrl
            );

            return user;
        }

        /*
         * STEP 2:
         *
         * Email linking is allowed only because the email was
         * verified by GitHub.
         */
        Optional<User> existingByEmail =
                userService.findByEmail(email);

        if (existingByEmail.isPresent()) {

            User user =
                    existingByEmail.get();

            /*
             * Never replace another GitHub identity.
             */
            if (user.getGithubId() != null
                    && !user.getGithubId().equals(githubId)) {

                throw new IllegalStateException(
                        "User account is already linked to another GitHub account"
                );
            }

            if (user.getGithubId() == null) {

                user.setGithubId(githubId);

                user =
                        userService.save(user);
            }

            /*
             * Existing user's roles are NEVER modified.
             */
            ensureProfileExists(
                    user,
                    profilePictureUrl
            );

            return user;
        }

        /*
         * STEP 3:
         *
         * Completely new UniShare user.
         */
        User newUser =
                new User();

        newUser.setGithubId(githubId);

        newUser.setEmail(email);

        /*
         * GitHub login name is only a username candidate.
         * UserService guarantees uniqueness.
         */
        newUser.setUsername(
                userService.generateUniqueUsername(
                        githubUsername
                )
        );

        newUser.setAuthProvider(
                AuthProvider.GITHUB
        );

        newUser.setActive(true);

        /*
         * IMPORTANT:
         *
         * RoleService obtains the default role from the
         * annotation-driven security declarations.
         *
         * No "USER" string is declared here.
         */
        newUser.setRoles(
                roleService.getDefaultUserRoles()
        );

        /*
         * OAuth users don't know this password.
         * It only satisfies the current User schema.
         */
        newUser.setPassword(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()
                )
        );

        User savedUser =
                userService.save(newUser);

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
                        .profilePictureUrl(profilePictureUrl)
                        .build();

        userProfileRepository.save(profile);
    }

    private String extractRequiredString(
            Map<String, Object> data,
            String key
    ) {

        Object value = data.get(key);

        if (value == null) {
            throw new IllegalStateException(
                    "GitHub response is missing: " + key
            );
        }

        String result =
                String.valueOf(value).trim();

        if (result.isBlank()) {
            throw new IllegalStateException(
                    "GitHub response contains empty: " + key
            );
        }

        return result;
    }

    private String extractOptionalString(
            Map<String, Object> data,
            String key
    ) {

        Object value = data.get(key);

        if (value == null) {
            return null;
        }

        String result =
                String.valueOf(value).trim();

        return result.isBlank()
                ? null
                : result;
    }

    private String normalizeEmail(String email) {

        return email
                .trim()
                .toLowerCase();
    }

    private ResponseCookie buildCookie(String jwt) {

        return ResponseCookie
                .from("token", jwt)
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite(
                        isProduction
                                ? "None"
                                : "Lax"
                )
                .build();
    }

    private ResponseEntity<Void> redirectToLoginError() {

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(
                        HttpHeaders.LOCATION,
                        frontendUrl + "/login-error"
                )
                .build();
    }
}