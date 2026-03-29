package com.elemental.backend.controller;


import com.elemental.backend.dto.OrderRequest;
import com.elemental.backend.dto.OrderResponse;
import com.elemental.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my/orders")
public class MyOrderController {

    private final OrderService orderService;

    public MyOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // GET /api/my/orders -> Mis pedidos
    @GetMapping
    public List<OrderResponse> myOrders(Authentication authentication) {
        String email = authentication.getName(); // viene del JWT
        return orderService.getMyOrders(email);
    }

    // GET /api/my/orders/{id} -> Detalle (solo si es del usuario)
    @GetMapping("/{id}")
    public OrderResponse myOrderDetail(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        return orderService.getMyOrderById(email, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request, Authentication authentication) {
        return orderService.createMyOrder(authentication.getName(), request);
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        return orderService.cancelMyOrder(email, id);
    }

}
