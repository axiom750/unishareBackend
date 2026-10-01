package com.unishare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.dto.response.api.UniEnvelope;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 401);
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", "Authentication required");
        errorDetails.put("path", request.getRequestURI());

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                "Authentication required"
        );

        response.getWriter().write(objectMapper.writeValueAsString(envelope));
    }
}
