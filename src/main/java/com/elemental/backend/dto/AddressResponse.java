package com.elemental.backend.dto;

import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        String street,
        String city,
        String postalCode,
        String country,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
