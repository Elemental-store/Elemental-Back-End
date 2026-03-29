package com.elemental.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        double total,
        LocalDateTime updatedAt
) {}
