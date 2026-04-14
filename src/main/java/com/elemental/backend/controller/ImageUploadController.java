package com.elemental.backend.controller;

import com.elemental.backend.entity.Product;
import com.elemental.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
public class ImageUploadController {

    private final ProductRepository productRepository;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ImageUploadController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String ext = getExtension(file.getOriginalFilename());
        String filename = "product-" + id + "-" + UUID.randomUUID() + ext;
        Path filePath = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = baseUrl + "/uploads/" + filename;
        product.setImageUrl(imageUrl);
        productRepository.save(product);

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
