package com.elemental.backend.controller.webhooks;

import com.elemental.backend.entity.Order;
import com.elemental.backend.entity.OrderStatus;
import com.elemental.backend.repository.OrderRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private final OrderRepository orderRepository;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    public StripeWebhookController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> handle(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        String type = event.getType();

        if ("payment_intent.succeeded".equals(type)) {
            PaymentIntent intent = getPaymentIntent(event);
            if (intent == null) return ResponseEntity.ok("ignored_no_intent");

            Order order = findOrder(intent);
            if (order == null) return ResponseEntity.ok("ignored_order_not_found");

            if (order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            }
            return ResponseEntity.ok("ok");
        }

        if ("payment_intent.payment_failed".equals(type)) {
            PaymentIntent intent = getPaymentIntent(event);
            if (intent == null) return ResponseEntity.ok("ignored_no_intent");

            Order order = findOrder(intent);
            if (order == null) return ResponseEntity.ok("ignored_order_not_found");

            if (order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.FAILED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            }
            return ResponseEntity.ok("ok");
        }

        // Eventos no manejados
        return ResponseEntity.ok("unhandled");
    }

    private PaymentIntent getPaymentIntent(Event event) {
        try {
            return (PaymentIntent) event.getDataObjectDeserializer()
                    .deserializeUnsafe();
        } catch (Exception e) {
            return null;
        }
    }

    private Order findOrder(PaymentIntent intent) {
        // 1) Por stripe_payment_intent_id
        Optional<Order> byIntentId = orderRepository.findByStripePaymentIntentId(intent.getId());
        if (byIntentId.isPresent()) return byIntentId.get();

        // 2) Fallback por metadata.orderId
        Map<String, String> metadata = intent.getMetadata();
        if (metadata != null) {
            String orderIdStr = metadata.get("orderId");
            if (orderIdStr != null && !orderIdStr.isBlank()) {
                try {
                    Long orderId = Long.parseLong(orderIdStr);
                    return orderRepository.findById(orderId).orElse(null);
                } catch (NumberFormatException ignored) { }
            }
        }
        return null;
    }
}
