package com.elemental.backend.controller;


import com.elemental.backend.dto.OrderRequest;
import com.elemental.backend.dto.OrderResponse;
import com.elemental.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public List<OrderResponse> myOrders(Authentication authentication) {
        String email = authentication.getName();
        return orderService.getMyOrders(email);
    }

    @GetMapping("/{id}")
    public OrderResponse myOrderDetail(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        return orderService.getMyOrderById(email, id);
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id, Authentication authentication) {
        byte[] pdf = orderService.generateMyOrderInvoicePdf(authentication.getName(), id);
        String filename = "Factura-ELEMENTAL-" + String.format("F%04d", id) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename)
                        .build()
                        .toString())
                .body(pdf);
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
