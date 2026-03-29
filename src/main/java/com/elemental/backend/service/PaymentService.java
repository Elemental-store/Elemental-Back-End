package com.elemental.backend.service;

import com.elemental.backend.dto.PaymentIntentResponse;

public interface PaymentService {
    PaymentIntentResponse createIntent(String userEmail, Long orderId);
}
