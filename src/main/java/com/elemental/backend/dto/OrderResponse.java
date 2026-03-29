package com.elemental.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String customerEmail;
    private Double totalAmount;
    private String status;
    private List<OrderItemResponse> items;
    private LocalDateTime createDate;

    public OrderResponse() {}

    public OrderResponse(Long id, String customerEmail, Double totalAmount,
                         String status, List<OrderItemResponse> items,
                         LocalDateTime createDate) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
        this.status = status;
        this.items = items;
        this.createDate = createDate;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }
}
