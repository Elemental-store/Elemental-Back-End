package com.elemental.backend.controller;

import com.elemental.backend.dto.AuthRequest;
import com.elemental.backend.dto.AuthResponse;
import com.elemental.backend.dto.RegisterRequest;
import com.elemental.backend.dto.ForgotPasswordRequest;
import com.elemental.backend.dto.ResetPasswordRequest;
import com.elemental.backend.service.AuthService;
import com.elemental.backend.service.PasswordResetService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService          = authService;
        this.passwordResetService = passwordResetService;
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

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetEmail(request.getEmail());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }
}