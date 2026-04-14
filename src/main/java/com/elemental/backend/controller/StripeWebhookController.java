package com.elemental.backend.controller;

import com.elemental.backend.entity.Order;
import com.elemental.backend.entity.OrderStatus;
import com.elemental.backend.entity.User;
import com.elemental.backend.repository.OrderRepository;
import com.elemental.backend.repository.UserRepository;
import com.elemental.backend.service.EmailService;
import com.elemental.backend.service.NotificationService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private final OrderRepository     orderRepository;
    private final UserRepository      userRepository;
    private final NotificationService notificationService;
    private final EmailService        emailService;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    public StripeWebhookController(OrderRepository orderRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService,
                                   EmailService emailService) {
        this.orderRepository     = orderRepository;
        this.userRepository      = userRepository;
        this.notificationService = notificationService;
        this.emailService        = emailService;
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

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent intent = getPaymentIntent(event);
            if (intent == null) return ResponseEntity.ok("ignored_no_intent");

            Order order = findOrder(intent);
            if (order == null) return ResponseEntity.ok("ignored_order_not_found");

            if (order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);

                String deliveryDate = calculateDeliveryDate();

                try {
                    notificationService.createOrderConfirmedNotification(
                            order.getCustomerEmail(), order.getId(), deliveryDate);
                } catch (Exception e) {
                    System.err.println("Error creando notificación: " + e.getMessage());
                }

                try {
                    User user = userRepository.findByEmail(order.getCustomerEmail())
                            .orElse(null);
                    emailService.sendOrderConfirmation(
                            order.getCustomerEmail(),
                            order.getId(),
                            order.getTotalAmount(),
                            deliveryDate,
                            order,
                            user
                    );
                } catch (Exception e) {
                    System.err.println("Error enviando email: " + e.getMessage());
                }
            }
            return ResponseEntity.ok("ok");
        }

        if ("payment_intent.payment_failed".equals(event.getType())) {
            PaymentIntent intent = getPaymentIntent(event);
            if (intent == null) return ResponseEntity.ok("ignored_no_intent");

            Order order = findOrder(intent);
            if (order != null && order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            }
            return ResponseEntity.ok("ok");
        }

        return ResponseEntity.ok("unhandled");
    }

    private String calculateDeliveryDate() {
        LocalDate date = LocalDate.now();
        int businessDays = 0;
        while (businessDays < 3) {
            date = date.plusDays(1);
            int dow = date.getDayOfWeek().getValue();
            if (dow != 6 && dow != 7) businessDays++;
        }
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private PaymentIntent getPaymentIntent(Event event) {
        try {
            return (PaymentIntent) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (Exception e) {
            return null;
        }
    }

    private Order findOrder(PaymentIntent intent) {
        Optional<Order> byIntentId = orderRepository.findByStripePaymentIntentId(intent.getId());
        if (byIntentId.isPresent()) return byIntentId.get();

        Map<String, String> metadata = intent.getMetadata();
        if (metadata != null) {
            String orderIdStr = metadata.get("orderId");
            if (orderIdStr != null && !orderIdStr.isBlank()) {
                try {
                    return orderRepository.findById(Long.parseLong(orderIdStr)).orElse(null);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}