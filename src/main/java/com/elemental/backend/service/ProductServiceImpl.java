package com.elemental.backend.service;

import com.elemental.backend.dto.ProductImageDto;
import com.elemental.backend.dto.ProductRequest;
import com.elemental.backend.dto.ProductResponse;
import com.elemental.backend.entity.Category;
import com.elemental.backend.entity.Product;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.CartItemRepository;
import com.elemental.backend.repository.CategoryRepository;
import com.elemental.backend.repository.FavoriteRepository;
import com.elemental.backend.repository.OrderDetailRepository;
import com.elemental.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final FavoriteRepository favoriteRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              CartItemRepository cartItemRepository,
                              OrderDetailRepository orderDetailRepository,
                              FavoriteRepository favoriteRepository) {
        this.productRepository    = productRepository;
        this.categoryRepository   = categoryRepository;
        this.cartItemRepository   = cartItemRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.favoriteRepository   = favoriteRepository;
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
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);

        return toResponse(productRepository.save(product));
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
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);

        return toResponse(productRepository.save(product));
    }

    @Override
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con id: " + id));

        favoriteRepository.deleteByProduct(product);
        cartItemRepository.deleteByProduct(product);
        orderDetailRepository.deleteByProduct(product);

        productRepository.delete(product);
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCreateDate(),
                product.getUpdateDate()
        );

        response.setDescription(product.getDescription());
        response.setImageUrl(product.getImageUrl());
        response.setImages(
                product.getImages().stream()
                        .map(i -> new ProductImageDto(i.getId(), i.getImageUrl(), i.getSortOrder()))
                        .toList()
        );

        return response;
    }
}