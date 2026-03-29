package com.elemental.backend.controller;

import com.elemental.backend.dto.OrderResponse;
import com.elemental.backend.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // GET /api/admin/orders -> Todos los pedidos
    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAll();
    }

    // GET /api/admin/orders/{id} -> Detalle de pedido
    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    // PUT /api/admin/orders/{id}/status?status=PAID (por ahora)
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @RequestParam String status) {
        return orderService.updateStatus(id, status);
    }
}
