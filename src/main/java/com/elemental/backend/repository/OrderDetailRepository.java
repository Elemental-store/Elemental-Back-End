package com.elemental.backend.repository;

import com.elemental.backend.entity.DetailsOrder;
import com.elemental.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository extends JpaRepository<DetailsOrder, Long> {

    @Modifying
    @Query("DELETE FROM DetailsOrder d WHERE d.product = :product")
    void deleteByProduct(@Param("product") Product product);
}