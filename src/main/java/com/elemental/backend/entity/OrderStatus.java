package com.elemental.backend.entity; // usa el mismo package que Role y PayMethod

public enum OrderStatus {
    PENDING,
    PAID,
    FAILED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}