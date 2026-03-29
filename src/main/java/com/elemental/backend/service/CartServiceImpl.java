package com.elemental.backend.service;

import com.elemental.backend.dto.*;
import com.elemental.backend.entity.Cart;
import com.elemental.backend.entity.CartItem;
import com.elemental.backend.entity.Product;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.CartItemRepository;
import com.elemental.backend.repository.CartRepository;
import com.elemental.backend.repository.ProductRepository;
import com.elemental.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getMyCart(String email) {
        Cart cart = cartRepository.findByUserEmailWithItems(email)
                .orElseGet(() -> createCart(email));
        return toResponse(cart);
    }

    @Override
    public CartResponse addItem(String email, AddCartItemRequest request) {

        Cart cart = cartRepository.findByUserEmailWithItems(email)
                .orElseGet(() -> createCart(email));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        // Si ya existe el mismo producto+talla, incrementa cantidad
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId())
                        && sameSize(i.getSize(), request.getSize()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            item.setSize(request.getSize());
            cart.getItems().add(item);
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Override
    public CartResponse updateItemQuantity(String email, Long itemId, UpdateCartItemRequest request) {

        CartItem item = cartItemRepository.findByIdAndCartUserEmail(itemId, email)
                .orElseThrow(() -> new NotFoundException("Item no encontrado"));

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart cart = cartRepository.findByUserEmailWithItems(email)
                .orElseThrow(() -> new NotFoundException("Carrito no encontrado"));

        return toResponse(cart);
    }

    @Override
    public CartResponse deleteItem(String email, Long itemId) {

        CartItem item = cartItemRepository.findByIdAndCartUserEmail(itemId, email)
                .orElseThrow(() -> new NotFoundException("Item no encontrado"));

        cartItemRepository.delete(item);

        Cart cart = cartRepository.findByUserEmailWithItems(email)
                .orElseThrow(() -> new NotFoundException("Carrito no encontrado"));

        return toResponse(cart);
    }

    private Cart createCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            double unitPrice = i.getProduct().getPrice();
            double subtotal = unitPrice * i.getQuantity();
            return new CartItemResponse(
                    i.getId(),
                    i.getProduct().getId(),
                    i.getProduct().getName(),
                    unitPrice,
                    i.getQuantity(),
                    i.getSize(),
                    subtotal
            );
        }).toList();

        double total = items.stream().mapToDouble(CartItemResponse::subtotal).sum();

        return new CartResponse(
                cart.getId(),
                items,
                total,
                cart.getUpdatedAt()
        );
    }

    private boolean sameSize(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }
}
