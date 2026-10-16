
package com.unishare.controller.auth;

import com.unishare.dto.request.rbac.ControlPlaneLoginRequest;
import com.unishare.dto.response.auth.ControlPlaneLoginResponse;
import com.unishare.service.auth.ControlPlaneAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/controlpanel")
@RequiredArgsConstructor
public class GodAccountController {

    private final ControlPlaneAuthService authService;

    @PostMapping("/login")
    public ControlPlaneLoginResponse login(
            @Valid @RequestBody ControlPlaneLoginRequest request
    ) {
        return authService.login(request);
    }
}
