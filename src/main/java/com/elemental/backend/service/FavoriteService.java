package com.elemental.backend.service;

import com.elemental.backend.dto.FavoriteResponse;
import java.util.List;

public interface FavoriteService {
    List<FavoriteResponse> getMyFavorites(String email, Long categoryId);
    void addFavorite(String email, Long productId);
    void removeFavorite(String email, Long productId);
    List<Long> getMyFavoriteIds(String email);
}