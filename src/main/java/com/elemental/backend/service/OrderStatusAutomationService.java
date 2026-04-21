package com.elemental.backend.service;

import com.elemental.backend.entity.Order;
import com.elemental.backend.entity.OrderStatus;
import com.elemental.backend.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderStatusAutomationService {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderStatusAutomationService(OrderRepository orderRepository,
                                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void advanceOrderStatuses() {
        movePaidOrdersToShipped();
        moveShippedOrdersToDelivered();
    }

    private void movePaidOrdersToShipped() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(2);
        List<Order> orders = orderRepository.findByStatusAndPaidAtBefore(OrderStatus.PAID, threshold);

        for (Order order : orders) {
            order.setStatus(OrderStatus.SHIPPED);
            notificationService.createOrderShippedNotification(order.getCustomerEmail(), order.getId());
        }

        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
        }
    }

    private void moveShippedOrdersToDelivered() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(5);
        List<Order> orders = orderRepository.findByStatusAndPaidAtBefore(OrderStatus.SHIPPED, threshold);

        for (Order order : orders) {
            order.setStatus(OrderStatus.DELIVERED);
            notificationService.createOrderDeliveredNotification(order.getCustomerEmail(), order.getId());
        }

        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
        }
    }
}
