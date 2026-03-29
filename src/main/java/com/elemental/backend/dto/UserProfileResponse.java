package com.elemental.backend.dto;

public record UserProfileResponse(
        Long id,
        String email,
        String role,
        String firstName,
        String lastName,
        String phone
) {}