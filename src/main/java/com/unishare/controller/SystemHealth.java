package com.unishare.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemHealth {

    @GetMapping("/health")
    public String getSystemHealth(){
        return "System is Running";
    }
}
