package com.elemental.backend.repository;

import com.elemental.backend.entity.Address;
import com.elemental.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.details d
        LEFT JOIN FETCH d.product
        WHERE o.id = :id
    """)
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.details d
        LEFT JOIN FETCH d.product
    """)
    List<Order> findAllWithDetails();

    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Modifying
    @Query("UPDATE Order o SET o.address = null WHERE o.address = :address")
    void nullifyAddress(@Param("address") Address address);
}
