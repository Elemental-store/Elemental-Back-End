package com.elemental.backend.service;

import com.elemental.backend.dto.PaymentIntentResponse;
import com.elemental.backend.entity.Order;
import com.elemental.backend.entity.OrderStatus;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.OrderRepository;
import com.elemental.backend.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository  userRepository;

    public PaymentServiceImpl(OrderRepository orderRepository,
                              UserRepository userRepository,
                              @Value("${stripe.secretKey}") String secretKey) {
        this.orderRepository = orderRepository;
        this.userRepository  = userRepository;
        Stripe.apiKey = secretKey;
    }

    @Override
    public PaymentIntentResponse createIntent(String userEmail, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));

        if (!order.getCustomerEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("No tienes permiso para pagar este pedido");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("El pedido no está en estado PENDING");
        }

        long amountCents = Math.round(order.getTotalAmount() * 100.0);

        try {
            // Obtener el stripeCustomerId del usuario si existe
            User user = userRepository.findByEmail(userEmail).orElse(null);
            String stripeCustomerId = (user != null) ? user.getStripeCustomerId() : null;

            PaymentIntentCreateParams.Builder builder =
                    PaymentIntentCreateParams.builder()
                            .setAmount(amountCents)
                            .setCurrency("eur")
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .setAllowRedirects(
                                                    PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                                            )
                                            .build()
                            )
                            .putMetadata("orderId", String.valueOf(order.getId()));

            if (stripeCustomerId != null && !stripeCustomerId.isEmpty()) {
                builder.setCustomer(stripeCustomerId);
            }

            PaymentIntent intent = PaymentIntent.create(builder.build());

            order.setStripePaymentIntentId(intent.getId());
            orderRepository.save(order);

            return new PaymentIntentResponse(intent.getClientSecret());

        } catch (Exception e) {
            throw new RuntimeException("Error creando PaymentIntent: " + e.getMessage(), e);
        }
    }
}