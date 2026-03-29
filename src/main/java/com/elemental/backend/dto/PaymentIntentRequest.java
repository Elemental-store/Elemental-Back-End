package com.elemental.backend.dto;

import jakarta.validation.constraints.NotNull;

public class PaymentIntentRequest {

    @NotNull
    private Long orderId;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
}
