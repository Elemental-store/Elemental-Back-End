package com.elemental.backend.service;

import com.elemental.backend.dto.OrderItemResponse;
import com.elemental.backend.dto.OrderRequest;
import com.elemental.backend.dto.OrderResponse;
import com.elemental.backend.entity.*;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.AddressRepository;
import com.elemental.backend.repository.CartRepository;
import com.elemental.backend.repository.OrderRepository;
import com.elemental.backend.repository.ProductRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;

    public OrderServiceImpl(CartRepository cartRepository,
                            OrderRepository orderRepository,
                            ProductRepository productRepository,
                            AddressRepository addressRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    public OrderResponse createMyOrder(String customerEmail, OrderRequest request) {

        Address address = addressRepository
                .findByIdAndUserEmail(request.getAddressId(), customerEmail)
                .orElseThrow(() -> new AccessDeniedException("Dirección no válida para este usuario"));

        Cart cart = cartRepository.findByUserEmailWithItems(customerEmail)
                .orElseThrow(() -> new NotFoundException("Carrito no encontrado"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }

        Order order = new Order();
        order.setCustomerEmail(customerEmail);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setPayMethod(request.getPayMethod());

        List<DetailsOrder> details = new ArrayList<>();
        double totalAmount = 0.0;

        for (CartItem cartItem : cart.getItems()) {

            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            if (product.getStock() < quantity) {
                throw new IllegalArgumentException("Stock insuficiente para: " + product.getName());
            }

            product.setStock(product.getStock() - quantity);
            productRepository.save(product);

            double unitPrice = product.getPrice();
            double subtotal = unitPrice * quantity;

            DetailsOrder detail = new DetailsOrder();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(unitPrice);
            detail.setSubtotal(subtotal);

            details.add(detail);
            totalAmount += subtotal;
        }

        order.setTotalAmount(totalAmount);
        order.setDetails(details);

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con id: " + id));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        return orderRepository.findAllWithDetails()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String customerEmail) {
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(String email, Long orderId) {

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con id: " + orderId));

        if (!order.getCustomerEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("No tienes permiso para ver este pedido");
        }

        return toResponse(order);
    }

    @Override
    public OrderResponse updateStatus(Long id, String status) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con id: " + id));

        String normalized = status == null ? "" : status.trim().toUpperCase();

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado no válido: " + status);
        }

        if (newStatus != OrderStatus.PENDING &&
                newStatus != OrderStatus.PAID &&
                newStatus != OrderStatus.FAILED &&
                newStatus != OrderStatus.SHIPPED &&
                newStatus != OrderStatus.DELIVERED &&
                newStatus != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Estado no permitido: " + status);
        }

        order.setStatus(newStatus);

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getDetails()
                .stream()
                .map(d -> new OrderItemResponse(
                        d.getProduct().getId(),
                        d.getProduct().getName(),
                        d.getQuantity(),
                        d.getUnitPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getTotalAmount(),
                order.getStatus() == null ? null : order.getStatus().name(),
                items,
                order.getCreateDate()
        );
    }
    @Override
    public OrderResponse cancelMyOrder(String customerEmail, Long orderId) {

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con id: " + orderId));

        if (!order.getCustomerEmail().equalsIgnoreCase(customerEmail)) {
            throw new AccessDeniedException("No tienes permiso para cancelar este pedido");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden cancelar pedidos en estado PENDIENTE");
        }

        for (DetailsOrder detail : order.getDetails()) {
            Product product = detail.getProduct();
            product.setStock(product.getStock() + detail.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }
}
