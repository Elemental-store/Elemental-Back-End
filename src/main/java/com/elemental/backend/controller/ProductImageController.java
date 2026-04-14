package com.elemental.backend.controller;

import com.elemental.backend.dto.ProductImageDto;
import com.elemental.backend.entity.Product;
import com.elemental.backend.entity.ProductImage;
import com.elemental.backend.repository.ProductImageRepository;
import com.elemental.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products/{productId}/images")
public class ProductImageController {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ProductImageController(ProductRepository productRepository,
                                  ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    @GetMapping
    public List<ProductImageDto> getImages(@PathVariable Long productId) {
        return productImageRepository.findByProductIdOrderBySortOrderAsc(productId)
                .stream()
                .map(i -> new ProductImageDto(i.getId(), i.getImageUrl(), i.getSortOrder()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProductImageDto> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String ext = getExtension(file.getOriginalFilename());
        String filename = "product-" + productId + "-" + UUID.randomUUID() + ext;
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = baseUrl + "/uploads/" + filename;

        int sortOrder = productImageRepository.findByProductIdOrderBySortOrderAsc(productId).size();

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(imageUrl);
        image.setSortOrder(sortOrder);
        ProductImage saved = productImageRepository.save(image);

        if (sortOrder == 0) {
            product.setImageUrl(imageUrl);
            productRepository.save(product);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProductImageDto(saved.getId(), saved.getImageUrl(), saved.getSortOrder()));
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable Long productId, @PathVariable Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));

        if (!image.getProduct().getId().equals(productId)) {
            throw new RuntimeException("La imagen no pertenece a este producto");
        }

        productImageRepository.delete(image);

        List<ProductImage> remaining = productImageRepository
                .findByProductIdOrderBySortOrderAsc(productId);

        Product product = productRepository.findById(productId).orElseThrow();
        product.setImageUrl(remaining.isEmpty() ? null : remaining.get(0).getImageUrl());
        productRepository.save(product);
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
