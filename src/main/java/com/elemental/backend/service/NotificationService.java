package com.elemental.backend.service;

import com.elemental.backend.entity.Notification;
import com.elemental.backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void createOrderConfirmedNotification(String userEmail, Long orderId, String deliveryDate) {
        Notification notification = new Notification();
        notification.setUserEmail(userEmail);
        notification.setTitle("Compra confirmada");
        notification.setMessage("Tu pedido #" + orderId + " ha sido confirmado. Llegará el " + deliveryDate + ".");
        notification.setType("ORDER_CONFIRMED");
        notification.setOrderId(orderId);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    public void createOrderShippedNotification(String userEmail, Long orderId) {
        Notification notification = new Notification();
        notification.setUserEmail(userEmail);
        notification.setTitle("Pedido en reparto");
        notification.setMessage("Tu pedido #" + orderId + " ya está en reparto.");
        notification.setType("ORDER_SHIPPED");
        notification.setOrderId(orderId);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    public void createOrderDeliveredNotification(String userEmail, Long orderId) {
        Notification notification = new Notification();
        notification.setUserEmail(userEmail);
        notification.setTitle("Pedido entregado");
        notification.setMessage("Tu pedido #" + orderId + " ha sido entregado.");
        notification.setType("ORDER_DELIVERED");
        notification.setOrderId(orderId);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(String userEmail) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    @Transactional(readOnly = true)
    public long countUnread(String userEmail) {
        return notificationRepository.countByUserEmailAndReadFalse(userEmail);
    }

    public void markAsRead(Long notificationId, String userEmail) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserEmail().equalsIgnoreCase(userEmail)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    public void markAllAsRead(String userEmail) {
        List<Notification> unread = notificationRepository
                .findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .filter(n -> !n.isRead())
                .toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
