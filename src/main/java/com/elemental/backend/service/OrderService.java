package com.elemental.backend.service;

import com.elemental.backend.dto.OrderRequest;
import com.elemental.backend.dto.OrderResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface OrderService {

    OrderResponse createMyOrder(String customerEmail, OrderRequest request);

    OrderResponse getMyOrderById(String email, Long OrderId);

    OrderResponse getById(Long id);

    List<OrderResponse> getAll();

    List<OrderResponse> getMyOrders(String customerEmail);

    OrderResponse updateStatus(@PathVariable Long id, @RequestParam String status);

    OrderResponse cancelMyOrder(String customerEmail, Long orderId);

    void delete(Long id);
}