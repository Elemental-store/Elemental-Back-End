package com.elemental.backend.dto;

public class ProductImageDto {
    private Long id;
    private String imageUrl;
    private String zoomImageUrl;
    private Integer sortOrder;

    public ProductImageDto(Long id, String imageUrl, String zoomImageUrl, Integer sortOrder) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.zoomImageUrl = zoomImageUrl;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getZoomImageUrl() { return zoomImageUrl; }
    public Integer getSortOrder() { return sortOrder; }
}
