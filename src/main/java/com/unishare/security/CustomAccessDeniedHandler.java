package com.unishare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.dto.response.api.UniEnvelope;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", 403);
        errorDetails.put("error", "Forbidden");
        errorDetails.put("message", "Access denied");
        errorDetails.put("path", request.getRequestURI());

        UniEnvelope<Map<String, Object>> envelope = new UniEnvelope<>(
                errorDetails,
                false,
                "Access denied"
        );

        response.getWriter().write(objectMapper.writeValueAsString(envelope));
    }
}
