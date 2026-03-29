package com.elemental.backend.service;

import com.elemental.backend.dto.ProductImageDto;
import com.elemental.backend.dto.ProductRequest;
import com.elemental.backend.dto.ProductResponse;
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
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        String name = request.getName().trim();

        if (productRepository.existsByName(name)) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada con id: " + request.getCategoryId()));

        Product product = new Product();
        product.setName(name);
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con id: " + id));
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getByCategoryId(Long categoryId) {
        // Opcional pero recomendable: validar que la categoría exista
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Categoría no encontrada con id: " + categoryId);
        }

        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con id: " + id));

        String newName = request.getName().trim();

        if (!product.getName().equalsIgnoreCase(newName) && productRepository.existsByName(newName)) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada con id: " + request.getCategoryId()));

        product.setName(newName);
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse r = new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCreateDate(),
                product.getUpdateDate()
        );
        r.setImageUrl(product.getImageUrl());
        r.setImages(product.getImages().stream()
                .map(i -> new ProductImageDto(i.getId(), i.getImageUrl(), i.getSortOrder()))
                .toList());
        return r;
    }

}
