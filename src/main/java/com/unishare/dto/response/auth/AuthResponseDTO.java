package com.unishare.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.unishare.dto.response.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {
    
    private boolean success;
    private String message;
    private UserDTO user;
}
