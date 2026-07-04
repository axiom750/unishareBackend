package com.unishare.service.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.unishare.entity.user.User;
import com.unishare.service.user.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final RestTemplate restTemplate;
    private final UserService userService;
    private final JwtService jwtService;

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


    public ResponseEntity<Void> handleCallback(String code) {

        try {
            String idToken = exchangeCodeForToken(code);
            GoogleIdToken.Payload payload = verifyToken(idToken);


            User user = userService.processGoogleUser(
                    (String) payload.get("name"),
                    payload.getEmail(),
                    payload.getSubject(),
                    (String) payload.get("picture")
            );

            String jwt = jwtService.generateToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            ResponseCookie cookie = buildCookie(jwt);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.LOCATION, frontendUrl)
                    .build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/login-error")
                    .build();
        }
    }

    private String exchangeCodeForToken(String code) {

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

        ResponseEntity<Map> response =
                restTemplate.postForEntity(tokenEndPoint, request, Map.class);

        if (response.getBody() == null ||
                response.getBody().get("id_token") == null) {
            throw new RuntimeException("Token response invalid");
        }

        return (String) response.getBody().get("id_token");
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
        return ResponseCookie.from("token", jwt)
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite(isProduction ? "None" : "Lax")
                .build();
    }
}