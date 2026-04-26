package com.elemental.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String customerEmail;
    private Double totalAmount;
    private String status;
    private String payMethod;
    private String cardBrand;
    private String cardLast4;
    private AddressResponse address;
    private List<OrderItemResponse> items;
    private LocalDateTime createDate;

    public OrderResponse() {}

    public OrderResponse(Long id, String customerEmail, Double totalAmount,
                         String status, String payMethod, String cardBrand, String cardLast4,
                         AddressResponse address, List<OrderItemResponse> items,
                         LocalDateTime createDate) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
        this.status = status;
        this.payMethod = payMethod;
        this.cardBrand = cardBrand;
        this.cardLast4 = cardLast4;
        this.address = address;
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

    public String getPayMethod() {
        return payMethod;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public AddressResponse getAddress() {
        return address;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }
}
