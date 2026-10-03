package com.unishare.config;

import com.unishare.controller.ride.RideController;
import com.unishare.controller.ride.RideRequestController;
import com.unishare.idempotency.IdempotencyService;
import com.unishare.security.CustomAccessDeniedHandler;
import com.unishare.security.CustomAuthenticationEntryPoint;
import com.unishare.service.auth.JwtService;
import com.unishare.service.ride.RideRequestService;
import com.unishare.service.ride.RideService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real SecurityConfig rules over HTTP (no database).
 *
 * User-scoped ride reads require authentication; the public ride listing does not.
 * Rule order matters: the specific authenticated GET matchers must win over the
 * broader public GET /api/rides/** rule.
 */
@WebMvcTest(controllers = {RideController.class, RideRequestController.class})
@Import({SecurityConfig.class, CorsConfig.class, CustomAccessDeniedHandler.class, CustomAuthenticationEntryPoint.class})
@TestPropertySource(properties = "frontend.url=http://localhost:3000")
@DisplayName("Ride endpoint access rules")
class RideEndpointSecurityTest {

    private static final String RIDE_ID = "01a1026a-654c-7c0b-a1cb-f4fde2de2a84";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private RideService rideService;
    @MockitoBean private RideRequestService rideRequestService;
    @MockitoBean private IdempotencyService idempotencyService;

    @Test
    @DisplayName("public ride listing stays public")
    void availableRidesArePublic() throws Exception {
        mockMvc.perform(get("/api/rides")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/rides/my-rides requires authentication")
    void myRidesRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/rides/my-rides")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/rides/my-requests requires authentication")
    void myRequestsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/rides/my-requests")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/rides/{rideId}/requests requires authentication")
    void rideRequestsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/rides/" + RIDE_ID + "/requests")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/rides/{rideId}/passengers (contains emails) requires authentication")
    void passengersRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/rides/" + RIDE_ID + "/passengers")).andExpect(status().isUnauthorized());
    }
}
