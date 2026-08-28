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


    public ResponseEntity<String> handleCallback(String code) {

        System.out.println("DEBUG: Google OAuth callback received with code: " + code.substring(0, Math.min(20, code.length())) + "...");

        try {
            String idToken = exchangeCodeForToken(code);
            System.out.println("DEBUG: Successfully exchanged code for token");
            
            GoogleIdToken.Payload payload = verifyToken(idToken);
            System.out.println("DEBUG: Token verified successfully for email: " + payload.getEmail());


            User user = userService.processGoogleUser(
                    (String) payload.get("name"),
                    payload.getEmail(),
                    payload.getSubject(),
                    (String) payload.get("picture")
            );
            System.out.println("DEBUG: User processed: " + user.getEmail());

            String jwt = jwtService.generateToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );
            System.out.println("DEBUG: JWT generated successfully");

            ResponseCookie cookie = buildCookie(jwt);

            System.out.println("DEBUG: Redirecting to: " + frontendUrl);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.LOCATION, frontendUrl)
                    .build();

        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Google OAuth error: " + e.getMessage());
            e.printStackTrace();
            
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

            System.out.println("DEBUG: Exchanging code for token with redirect_uri: " + redirectUri);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(tokenEndPoint, request, Map.class);

            System.out.println("DEBUG: Token exchange response: " + response.getStatusCode());

            if (response.getBody() == null) {
                System.err.println("ERROR: Token response body is null");
                throw new RuntimeException("Token response body is null");
            }

            if (response.getBody().get("id_token") == null) {
                System.err.println("ERROR: Token response does not contain id_token. Response: " + response.getBody());
                throw new RuntimeException("Token response invalid - no id_token");
            }

            return (String) response.getBody().get("id_token");
        } catch (Exception e) {
            System.err.println("ERROR in exchangeCodeForToken: " + e.getMessage());
            e.printStackTrace();
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
        ResponseCookie cookie = ResponseCookie.from("token", jwt)
                .httpOnly(true)
                .secure(isProduction)
                .domain("localhost")  // Set for entire localhost domain
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite(isProduction ? "None" : "Lax")
                .build();
        
        System.out.println("DEBUG: Building cookie - httpOnly: true, secure: " + isProduction + ", domain: localhost, sameSite: " + (isProduction ? "None" : "Lax"));
        System.out.println("DEBUG: Cookie: " + cookie.toString());
        
        return cookie;
    }
}