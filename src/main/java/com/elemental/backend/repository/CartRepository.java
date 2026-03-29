package com.elemental.backend.repository;

import com.elemental.backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserEmail(String email);

    @Query("""
        select c from Cart c
        left join fetch c.items i
        left join fetch i.product p
        where c.user.email = :email
    """)
    Optional<Cart> findByUserEmailWithItems(String email);
}
