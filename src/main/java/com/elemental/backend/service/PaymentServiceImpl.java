package com.elemental.backend.service;

import com.elemental.backend.dto.PaymentIntentResponse;
import com.elemental.backend.entity.Order;
import com.elemental.backend.entity.OrderStatus;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.service.EmailService;
import com.elemental.backend.service.NotificationService;
import com.elemental.backend.repository.OrderRepository;
import com.elemental.backend.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository  userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final StripeService stripeService;

    public PaymentServiceImpl(OrderRepository orderRepository,
                              UserRepository userRepository,
                              NotificationService notificationService,
                              EmailService emailService,
                              StripeService stripeService,
                              @Value("${stripe.secretKey}") String secretKey) {
        this.orderRepository = orderRepository;
        this.userRepository  = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.stripeService = stripeService;
        Stripe.apiKey = secretKey;
    }

    @Override
    public PaymentIntentResponse createIntent(String userEmail, Long orderId, boolean savePaymentMethod) {

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
            User user = userRepository.findByEmail(userEmail).orElse(null);
            String stripeCustomerId = getStripeCustomerId(user, savePaymentMethod);

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

            if (savePaymentMethod) {
                builder.setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION);
            }

            PaymentIntent intent = PaymentIntent.create(builder.build());

            order.setStripePaymentIntentId(intent.getId());
            orderRepository.save(order);

            return new PaymentIntentResponse(intent.getClientSecret());

        } catch (Exception e) {
            throw new RuntimeException("Error creando PaymentIntent: " + e.getMessage(), e);
        }
    }

    private String getStripeCustomerId(User user, boolean requiredForSavedPaymentMethod) {
        if (user == null) return null;
        if (!isBlank(user.getStripeCustomerId())) return user.getStripeCustomerId();
        if (!requiredForSavedPaymentMethod) return null;

        String name = ((user.getFirstName() != null ? user.getFirstName() : "")
                + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        String customerId = stripeService.createCustomer(user.getEmail(), name);
        user.setStripeCustomerId(customerId);
        userRepository.save(user);
        return customerId;
    }

    @Override
    public void confirmSuccessfulPayment(String userEmail, Long orderId, String paymentMethodId, String cardBrand, String cardLast4) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));

        if (!order.getCustomerEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("No tienes permiso para confirmar este pedido");
        }

        String paymentIntentId = order.getStripePaymentIntentId();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalStateException("El pedido no tiene un PaymentIntent asociado");
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            if (!"succeeded".equalsIgnoreCase(intent.getStatus())) {
                throw new IllegalStateException("El pago aún no se ha completado");
            }
            markOrderAsPaid(order, paymentMethodId, cardBrand, cardLast4);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error verificando el pago: " + e.getMessage(), e);
        }
    }

    private void markOrderAsPaid(Order order, String paymentMethodId, String cardBrand, String cardLast4) {
        applyProvidedSnapshot(order, cardBrand, cardLast4);
        enrichCardSnapshot(order, order.getStripePaymentIntentId(), paymentMethodId);

        if (order.getStatus() == OrderStatus.PAID) {
            orderRepository.save(order);
            return;
        }

        order.setStatus(OrderStatus.PAID);
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        orderRepository.save(order);

        String deliveryDate = calculateDeliveryDate();

        try {
            notificationService.createOrderConfirmedNotification(
                    order.getCustomerEmail(), order.getId(), deliveryDate);
        } catch (Exception e) {
            System.err.println("Error creando notificación: " + e.getMessage());
        }

        try {
            User user = userRepository.findByEmail(order.getCustomerEmail()).orElse(null);
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

    private void enrichCardSnapshot(Order order, String paymentIntentId, String providedPaymentMethodId) {
        try {
            String paymentMethodId = providedPaymentMethodId;
            if (isBlank(paymentMethodId)) {
                if (isBlank(paymentIntentId)) return;
                PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
                paymentMethodId = intent.getPaymentMethod();
            }
            if (isBlank(paymentMethodId)) return;

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            if (paymentMethod.getCard() == null) return;

            order.setCardBrand(paymentMethod.getCard().getBrand());
            order.setCardLast4(paymentMethod.getCard().getLast4());
        } catch (Exception e) {
            System.err.println("Error guardando snapshot de tarjeta: " + e.getMessage());
        }
    }

    private void applyProvidedSnapshot(Order order, String cardBrand, String cardLast4) {
        if (!isBlank(cardBrand)) {
            order.setCardBrand(cardBrand);
        }
        if (!isBlank(cardLast4)) {
            order.setCardLast4(cardLast4);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
}
