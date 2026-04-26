package com.elemental.backend.repository;

import com.elemental.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndParent(String name, Category parent);

    List<Category> findByParent(Category parent);
}