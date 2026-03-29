package com.elemental.backend.service;

import com.elemental.backend.dto.FavoriteResponse;
import com.elemental.backend.entity.Favorite;
import com.elemental.backend.entity.Product;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.FavoriteRepository;
import com.elemental.backend.repository.ProductRepository;
import com.elemental.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public FavoriteServiceImpl(FavoriteRepository favoriteRepository,
                               ProductRepository productRepository,
                               UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository  = productRepository;
        this.userRepository     = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteResponse> getMyFavorites(String email, Long categoryId) {
        List<Favorite> favorites = categoryId != null
                ? favoriteRepository.findByUserEmailAndProductCategoryId(email, categoryId)
                : favoriteRepository.findByUserEmail(email);

        return favorites.stream().map(f -> {
            Product p = f.getProduct();
            return new FavoriteResponse(
                    p.getId(),
                    p.getName(),
                    p.getPrice(),
                    p.getImageUrl(),
                    p.getCategory() != null ? p.getCategory().getId() : null,
                    p.getCategory() != null ? p.getCategory().getName() : null
            );
        }).toList();
    }

    @Override
    public void addFavorite(String email, Long productId) {
        if (favoriteRepository.existsByUserEmailAndProductId(email, productId)) return;

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        favoriteRepository.save(favorite);
    }

    @Override
    public void removeFavorite(String email, Long productId) {
        favoriteRepository.deleteByUserEmailAndProductId(email, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMyFavoriteIds(String email) {
        return favoriteRepository.findByUserEmail(email)
                .stream()
                .map(f -> f.getProduct().getId())
                .toList();
    }
}