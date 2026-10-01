package com.unishare.controller;


import com.unishare.dto.response.MessageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemHealth {

    @GetMapping("/health")
    public MessageResponse getSystemHealth(){
        return new MessageResponse("System is Running");
    }
}
