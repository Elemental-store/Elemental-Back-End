package com.elemental.backend.controller;

import com.elemental.backend.dto.AuthRequest;
import com.elemental.backend.dto.AuthResponse;
import com.elemental.backend.dto.RegisterRequest;
import com.elemental.backend.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value="/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request, false);
    }

    @PostMapping(value="/register-admin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse registerAdmin(@RequestBody RegisterRequest request) {
        return authService.register(request, true);
    }

    @PostMapping(value="/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

}
