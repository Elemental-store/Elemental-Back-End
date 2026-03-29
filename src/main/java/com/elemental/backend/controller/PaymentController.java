package com.elemental.backend.controller;

import com.elemental.backend.dto.PaymentIntentRequest;
import com.elemental.backend.dto.PaymentIntentResponse;
import com.elemental.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/intent")
    public PaymentIntentResponse createIntent(@Valid @RequestBody PaymentIntentRequest request,
                                              Authentication authentication) {
        return paymentService.createIntent(authentication.getName(), request.getOrderId());
    }
}
