package com.elemental.backend.controller;

import com.elemental.backend.dto.ProductImageDto;
import com.elemental.backend.entity.Product;
import com.elemental.backend.entity.ProductImage;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.ProductImageRepository;
import com.elemental.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products/{productId}/images")
public class ProductImageController {

    private static final int DISPLAY_MAX_WIDTH = 1600;
    private static final float JPEG_QUALITY = 0.9f;

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
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProductImageDto> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen está vacío");
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String extension = normalizeExtension(file.getOriginalFilename());
        String token = "product-" + productId + "-" + UUID.randomUUID();
        String zoomFilename = token + "-zoom" + extension;
        String displayFilename = token + "-display" + extension;

        Path zoomPath = uploadPath.resolve(zoomFilename);
        Path displayPath = uploadPath.resolve(displayFilename);

        BufferedImage source = readImage(file);
        saveImage(source, zoomPath, extension, false);
        saveImage(source, displayPath, extension, true);

        String zoomImageUrl = buildImageUrl(zoomFilename);
        String imageUrl = buildImageUrl(displayFilename);
        int sortOrder = productImageRepository.findByProductIdOrderBySortOrderAsc(productId).size();

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(imageUrl);
        image.setZoomImageUrl(zoomImageUrl);
        image.setSortOrder(sortOrder);
        ProductImage saved = productImageRepository.save(image);

        if (sortOrder == 0) {
            product.setImageUrl(imageUrl);
            productRepository.save(product);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PatchMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderImages(@PathVariable Long productId,
                              @RequestBody List<Long> orderedIds) {

        List<ProductImage> images = productImageRepository
                .findByProductIdOrderBySortOrderAsc(productId);

        for (int i = 0; i < orderedIds.size(); i++) {
            final int index = i;
            final Long imageId = orderedIds.get(i);
            images.stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .ifPresent(img -> img.setSortOrder(index));
        }

        productImageRepository.saveAll(images);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        orderedIds.stream().findFirst().flatMap(firstId ->
                images.stream().filter(img -> img.getId().equals(firstId)).findFirst()
        ).ifPresent(first -> {
            product.setImageUrl(first.getImageUrl());
            productRepository.save(product);
        });
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable Long productId, @PathVariable Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Imagen no encontrada"));

        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("La imagen no pertenece a este producto");
        }

        productImageRepository.delete(image);

        List<ProductImage> remaining = productImageRepository
                .findByProductIdOrderBySortOrderAsc(productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        product.setImageUrl(remaining.isEmpty() ? null : remaining.get(0).getImageUrl());
        productRepository.save(product);
    }

    private ProductImageDto toDto(ProductImage image) {
        return new ProductImageDto(
                image.getId(),
                image.getImageUrl(),
                image.getZoomImageUrl() != null ? image.getZoomImageUrl() : image.getImageUrl(),
                image.getSortOrder()
        );
    }

    private BufferedImage readImage(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("El archivo no es una imagen válida");
            }
            return image;
        }
    }

    private void saveImage(BufferedImage source, Path target, String extension, boolean resizeForDisplay) throws IOException {
        BufferedImage output = resizeForDisplay ? scaleDown(source, DISPLAY_MAX_WIDTH) : source;

        if (".png".equals(extension)) {
            ImageIO.write(output, "png", target.toFile());
            return;
        }

        writeJpeg(output, target);
    }

    private BufferedImage scaleDown(BufferedImage source, int maxWidth) {
        if (source.getWidth() <= maxWidth) {
            return source;
        }

        int targetWidth = maxWidth;
        int targetHeight = (int) Math.round((double) source.getHeight() * targetWidth / source.getWidth());
        int imageType = source.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private void writeJpeg(BufferedImage image, Path target) throws IOException {
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(rgbImage, "jpg", target.toFile());
            return;
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(outputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(rgbImage, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private String normalizeExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        String extension = dot >= 0 ? filename.substring(dot).toLowerCase(Locale.ROOT) : ".jpg";
        return ".png".equals(extension) ? ".png" : ".jpg";
    }

    private String buildImageUrl(String filename) {
        return baseUrl + "/uploads/" + filename;
    }
}
