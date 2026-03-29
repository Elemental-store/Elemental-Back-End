package com.elemental.backend.service;

import com.elemental.backend.dto.ProductRequest;
import com.elemental.backend.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse getById(Long id);

    List<ProductResponse> getAll();

    List<ProductResponse> getByCategoryId(Long categoryId);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
