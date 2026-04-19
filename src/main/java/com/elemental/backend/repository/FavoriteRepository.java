package com.elemental.backend.repository;

import com.elemental.backend.entity.Favorite;
import com.elemental.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserEmail(String email);
    List<Favorite> findByUserEmailAndProductCategoryId(String email, Long categoryId);
    Optional<Favorite> findByUserEmailAndProductId(String email, Long productId);
    boolean existsByUserEmailAndProductId(String email, Long productId);
    void deleteByUserEmailAndProductId(String email, Long productId);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.product = :product")
    void deleteByProduct(@Param("product") Product product);
}