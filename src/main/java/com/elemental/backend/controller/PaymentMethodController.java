package com.elemental.backend.controller;

import com.elemental.backend.entity.User;
import com.elemental.backend.repository.UserRepository;
import com.elemental.backend.service.StripeService;
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
    private final UserRepository userRepository;

    public PaymentMethodController(StripeService stripeService,
                                   UserRepository userRepository) {
        this.stripeService  = stripeService;
        this.userRepository = userRepository;
    }

    // GET /api/my/payment-methods — listar tarjetas guardadas
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        if (user.getStripeCustomerId() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(stripeService.listPaymentMethods(user.getStripeCustomerId()));
    }

    // POST /api/my/payment-methods/setup-intent — obtener clientSecret para añadir tarjeta
    @PostMapping("/setup-intent")
    public ResponseEntity<Map<String, String>> setupIntent(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        // Si no tiene Customer en Stripe, lo creamos ahora
        if (user.getStripeCustomerId() == null) {
            String name = (user.getFirstName() != null ? user.getFirstName() : "")
                    + " " + (user.getLastName() != null ? user.getLastName() : "");
            String customerId = stripeService.createCustomer(user.getEmail(), name.trim());
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }

        String clientSecret = stripeService.createSetupIntent(user.getStripeCustomerId());
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    // DELETE /api/my/payment-methods/:id — eliminar tarjeta
    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Void> delete(
            @PathVariable String paymentMethodId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Verificamos que el usuario tenga customer antes de eliminar
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