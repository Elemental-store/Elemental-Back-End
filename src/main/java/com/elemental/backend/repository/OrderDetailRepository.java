package com.elemental.backend.repository;

import com.elemental.backend.entity.DetailsOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository extends JpaRepository<DetailsOrder, Long> {
}
