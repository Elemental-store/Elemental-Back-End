package com.elemental.backend.repository;

import com.elemental.backend.entity.CartItem;
import com.elemental.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCartUserEmail(Long id, String email);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.product = :product")
    void deleteByProduct(@Param("product") Product product);
}