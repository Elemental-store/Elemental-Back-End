package com.elemental.backend.service;

import com.elemental.backend.dto.AuthRequest;
import com.elemental.backend.dto.AuthResponse;
import com.elemental.backend.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, boolean admin);
    AuthResponse login(AuthRequest request);
}
