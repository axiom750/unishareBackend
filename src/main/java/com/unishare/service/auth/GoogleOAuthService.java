package com.unishare.service.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.unishare.entity.user.User;
import com.unishare.service.rbac.RoleService;
import com.unishare.service.user.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final RestTemplate restTemplate;
    private final UserService userService;
    private final JwtService jwtService;
    private final RoleService roleService;

    private GoogleIdTokenVerifier verifier;

    @Value("${google.redirectURL}")
    private String redirectUri;

    @Value("${google.clientSecret}")
    private String clientSecret;

    @Value("${google.clientId}")
    private String clientId;

    @Value("${google.auth.endPoint}")
    private String tokenEndPoint;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${production.status}")
    private boolean isProduction;

    @PostConstruct
    public void init() {
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        )
                .setAudience(Collections.singletonList(clientId))
                .setIssuer("https://accounts.google.com")
                .build();
    }

    public ResponseEntity<String> handleCallback(String code) {

        log.debug("Google OAuth callback received");

        try {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Google authorization code is missing");
            }

            String idToken = exchangeCodeForToken(code);
            log.debug("Successfully exchanged code for token");

            GoogleIdToken.Payload payload = verifyToken(idToken);
            log.debug("Token verified successfully for email: {}", payload.getEmail());

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new IllegalStateException("Google account email is not verified");
            }

            User user = userService.processGoogleUser(
                    (String) payload.get("name"),
                    payload.getEmail(),
                    payload.getSubject(),
                    (String) payload.get("picture")
            );
            log.debug("User processed: {}", user.getEmail());

            String primaryRole = roleService.getPrimaryRoleNameByUserId(user.getId());

            String jwt = jwtService.generateToken(
                    user.getId(),
                    user.getEmail(),
                    primaryRole
            );
            log.debug("JWT generated successfully");

            ResponseCookie cookie = buildCookie(jwt);

            log.debug("Redirecting to: {}", frontendUrl);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.LOCATION, frontendUrl)
                    .build();

        } catch (Exception e) {
            log.error("[OAUTH] Google authentication failed", e);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/login-error?error=oauth_failed")
                    .build();
        }
    }

    private String exchangeCodeForToken(String code) {

        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(params, headers);

            log.debug("Exchanging code for token with redirect_uri: {}", redirectUri);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(tokenEndPoint, request, Map.class);

            log.debug("Token exchange response status: {}", response.getStatusCode());

            if (response.getBody() == null) {
                log.error("Token response body is null");
                throw new RuntimeException("Token response body is null");
            }

            if (response.getBody().get("id_token") == null) {
                log.error("Token response does not contain id_token");
                throw new RuntimeException("Token response invalid - no id_token");
            }

            return (String) response.getBody().get("id_token");
        } catch (Exception e) {
            log.error("Failed to exchange code for token", e);
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage(), e);
        }
    }

    private GoogleIdToken.Payload verifyToken(String idTokenString)
            throws Exception {

        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            throw new RuntimeException("Invalid Google ID Token");
        }

        return idToken.getPayload();
    }

    private ResponseCookie buildCookie(String jwt) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("token", jwt)
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite(isProduction ? "None" : "Lax");

        // Only set domain for local development
        if (!isProduction) {
            cookieBuilder.domain("localhost");
        }
        // For production, don't set domain to allow cross-domain cookies

        ResponseCookie cookie = cookieBuilder.build();

        log.debug("JWT authentication cookie created successfully - httpOnly: true, secure: {}, domain: {}, sameSite: {}",
                isProduction,
                isProduction ? "not-set (cross-domain)" : "localhost",
                isProduction ? "None" : "Lax");

        return cookie;
    }
}