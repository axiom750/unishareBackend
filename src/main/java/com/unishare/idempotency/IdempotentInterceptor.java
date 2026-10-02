package com.unishare.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent annotation = handlerMethod.getMethodAnnotation(Idempotent.class);

        // Endpoint is not marked @Idempotent
        // Do not change its existing behavior
        if (annotation == null) {
            return true;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn(
                    "[IDEMPOTENCY] Missing Idempotency-Key | method={} | uri={}",
                    request.getMethod(),
                    request.getRequestURI()
            );
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required for this operation"
            );
        }

        // Add prefix for Redis namespacing
        String redisKey = "idempotency:" + idempotencyKey;
        
        boolean reserved = idempotencyService.reserve(redisKey);

        if (!reserved) {
            log.warn(
                    "[IDEMPOTENCY] Duplicate request | method={} | uri={}",
                    request.getMethod(),
                    request.getRequestURI()
            );
            throw new IllegalStateException(
                    "Duplicate request. This operation has already been processed or is currently being processed."
            );
        }

        log.info(
                "[IDEMPOTENCY] Request reserved | method={} | uri={}",
                request.getMethod(),
                request.getRequestURI()
        );

        return true;
    }
}
