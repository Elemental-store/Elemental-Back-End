package com.elemental.backend.dto;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        double unitPrice,
        int quantity,
        String size,
        double subtotal
) {}
