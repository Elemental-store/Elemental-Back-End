package com.elemental.backend.controller;

import com.elemental.backend.dto.SavePaymentMethodRequest;
import com.elemental.backend.entity.User;
import com.elemental.backend.repository.UserRepository;
import com.elemental.backend.service.StripeCustomerService;
import com.elemental.backend.service.StripeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/my/payment-methods")
public class PaymentMethodController {

    private final StripeService stripeService;
    private final StripeCustomerService stripeCustomerService;
    private final UserRepository userRepository;

    public PaymentMethodController(StripeService stripeService,
                                   StripeCustomerService stripeCustomerService,
                                   UserRepository userRepository) {
        this.stripeService  = stripeService;
        this.stripeCustomerService = stripeCustomerService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        if (user.getStripeCustomerId() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(stripeService.listPaymentMethods(user.getStripeCustomerId()));
    }

    @PostMapping("/setup-intent")
    public ResponseEntity<Map<String, String>> setupIntent(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        stripeCustomerService.ensureCustomer(user);

        String clientSecret = stripeService.createSetupIntent(user.getStripeCustomerId());
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    @PostMapping
    public ResponseEntity<Void> save(
            @Valid @RequestBody SavePaymentMethodRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        stripeCustomerService.ensureCustomer(user);
        stripeService.attachPaymentMethod(request.getPaymentMethodId(), user.getStripeCustomerId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Void> delete(
            @PathVariable String paymentMethodId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        if (user.getStripeCustomerId() == null) {
            return ResponseEntity.badRequest().build();
        }

        stripeService.detachPaymentMethod(paymentMethodId);
        return ResponseEntity.noContent().build();
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

}
