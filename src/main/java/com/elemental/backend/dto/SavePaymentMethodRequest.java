package com.elemental.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class SavePaymentMethodRequest {

    @NotBlank
    private String paymentMethodId;

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }
}
