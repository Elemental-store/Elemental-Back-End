package com.elemental.backend.service;

import com.elemental.backend.dto.CategoryRequest;
import com.elemental.backend.dto.CategoryResponse;
import com.elemental.backend.entity.Category;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        String name = request.getName().trim();

        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada con id: " + id));

        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada con id: " + id));

        String newName = request.getName().trim();

        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByName(newName)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }

        category.setName(newName);
        category.setDescription(request.getDescription());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Categoría no encontrada con id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreateDate(),
                category.getUpdateDate()
        );
    }
}
