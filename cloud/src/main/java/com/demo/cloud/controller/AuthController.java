package com.demo.cloud.controller;

import com.demo.cloud.service.AuthService;
import com.demo.cloud.dto.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest req) {
        authService.loginLocal(req.email(), req.password());
        return ResponseEntity.ok("Login OK");
    }
}