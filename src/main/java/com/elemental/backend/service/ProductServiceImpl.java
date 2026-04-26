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

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    public List<ProductResponse> search(String query, Integer limit) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return getAll();
        }

        List<String> terms = Arrays.stream(normalizedQuery.split("\\s+"))
                .filter(term -> !term.isBlank())
                .distinct()
                .toList();

        return productRepository.findAll()
                .stream()
                .filter(product -> matches(product, terms))
                .sorted(
                        Comparator
                                .comparingInt((Product product) -> relevanceScore(product, normalizedQuery))
                                .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER)
                )
                .limit(resolveLimit(limit))
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

    private boolean matches(Product product, List<String> terms) {
        String searchable = searchableText(product);
        return terms.stream().allMatch(searchable::contains);
    }

    private int relevanceScore(Product product, String query) {
        String name = normalize(product.getName());
        String categoryName = normalize(product.getCategory().getName());
        String description = normalize(product.getDescription());

        int nameScore = fieldScore(name, query, 0);
        int categoryScore = fieldScore(categoryName, query, 40);
        int descriptionScore = fieldScore(description, query, 80);

        int score = Math.min(nameScore, Math.min(categoryScore, descriptionScore));
        if (score == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return score + Math.min(name.length(), 60);
    }

    private int fieldScore(String field, String query, int baseScore) {
        if (field.isBlank()) {
            return Integer.MAX_VALUE;
        }
        if (field.equals(query)) {
            return baseScore;
        }
        if (field.startsWith(query)) {
            return baseScore + 10;
        }

        int index = field.indexOf(query);
        if (index >= 0) {
            return baseScore + 20 + Math.min(index, 40);
        }

        return Integer.MAX_VALUE;
    }

    private String searchableText(Product product) {
        return String.join(" ",
                normalize(product.getName()),
                normalize(product.getCategory().getName()),
                normalize(product.getDescription())
        ).trim();
    }

    private long resolveLimit(Integer limit) {
        if (limit == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(1, Math.min(limit, 20));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized;
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
                        .map(i -> new ProductImageDto(
                                i.getId(),
                                i.getImageUrl(),
                                i.getZoomImageUrl() != null ? i.getZoomImageUrl() : i.getImageUrl(),
                                i.getSortOrder()
                        ))
                        .toList()
        );

        return response;
    }
}
