package com.elemental.backend.dto;

public record FavoriteResponse(
        Long productId,
        String name,
        Double price,
        String imageUrl,
        Long categoryId,
        String categoryName
) {}