package com.elemental.backend.controller;

import com.elemental.backend.dto.FavoriteResponse;
import com.elemental.backend.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/my/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteResponse> getMyFavorites(
            @RequestParam(required = false) Long categoryId,
            Authentication authentication) {
        return favoriteService.getMyFavorites(authentication.getName(), categoryId);
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void addFavorite(@PathVariable Long productId, Authentication authentication) {
        favoriteService.addFavorite(authentication.getName(), productId);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable Long productId, Authentication authentication) {
        favoriteService.removeFavorite(authentication.getName(), productId);
    }

    @GetMapping("/ids")
    public List<Long> getMyFavoriteIds(Authentication authentication) {
        return favoriteService.getMyFavoriteIds(authentication.getName());
    }
}