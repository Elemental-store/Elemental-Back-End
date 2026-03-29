package com.elemental.backend.dto;

public class ProductImageDto {
    private Long id;
    private String imageUrl;
    private Integer sortOrder;

    public ProductImageDto(Long id, String imageUrl, Integer sortOrder) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public Integer getSortOrder() { return sortOrder; }
}
