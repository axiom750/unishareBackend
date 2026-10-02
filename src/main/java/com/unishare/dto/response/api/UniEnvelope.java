package com.unishare.dto.response.api;

import lombok.*;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class UniEnvelope<T> {

    private T data;
    private Meta meta;

    public UniEnvelope() {
    }

    public UniEnvelope(T data) {
        this.data = data;
        this.meta = new Meta();
    }

    public UniEnvelope(T data, boolean success, String message) {
        this.data = data;
        this.meta = new Meta(success, message);
    }

    @Getter
    @Setter
    public static class Meta {

        private boolean success;
        private LocalDateTime timestamp;
        private String traceId;
        private String message;

        public Meta() {
            this.success = true;
            this.timestamp = LocalDateTime.now();
            this.traceId = getCurrentTraceId();
            this.message = "Thank you for using Unishare";
        }

        public Meta(boolean success, String message) {
            this.success = success;
            this.timestamp = LocalDateTime.now();
            this.traceId = getCurrentTraceId();
            this.message = message;
        }

        private String getCurrentTraceId() {
            String traceId = MDC.get("traceId");

            return traceId != null
                    ? traceId
                    : UUID.randomUUID().toString();
        }
    }
}