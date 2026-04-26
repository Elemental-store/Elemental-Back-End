package com.elemental.backend.service;

import com.elemental.backend.dto.CategoryRequest;
import com.elemental.backend.dto.CategoryResponse;
import com.elemental.backend.entity.Category;
import com.elemental.backend.entity.Product;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.CategoryRepository;
import com.elemental.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductService     productService;
    private final ProductRepository  productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               ProductService productService,
                               ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productService     = productService;
        this.productRepository  = productRepository;
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        String name = request.getName().trim();

        Category parent = resolveParent(request.getParentId());

        if (categoryRepository.existsByNameAndParent(name, parent)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre en esta categoría padre");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription());
        category.setParent(parent);

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada con id: " + id)));
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

        Category parent = resolveParent(request.getParentId());

        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new IllegalArgumentException("Una categoría no puede ser su propio padre");
        }

        if (!category.getName().equalsIgnoreCase(newName)
                && categoryRepository.existsByNameAndParent(newName, parent)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre en esta categoría padre");
        }

        category.setName(newName);
        category.setDescription(request.getDescription());
        category.setParent(parent);

        return toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada con id: " + id));

        List<Category> subcategories = categoryRepository.findByParent(category);
        for (Category sub : subcategories) {
            sub.setParent(null);
            categoryRepository.save(sub);
        }

        List<Product> products = productRepository.findByCategory(category);
        for (Product product : products) {
            productService.delete(product.getId());
        }

        categoryRepository.delete(category);
    }

    private Category resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Categoría padre no encontrada con id: " + parentId));
    }

    private CategoryResponse toResponse(Category category) {
        Long parentId = category.getParent() != null ? category.getParent().getId() : null;
        return new CategoryResponse(
                category.getId(),
                parentId,
                category.getName(),
                category.getDescription(),
                category.getCreateDate(),
                category.getUpdateDate()
        );
    }
}
