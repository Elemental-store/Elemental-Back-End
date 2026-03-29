package com.elemental.backend.controller;

import com.elemental.backend.dto.*;
import com.elemental.backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my/cart")
public class MyCartController {

    private final CartService cartService;

    public MyCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse myCart(Authentication authentication) {
        return cartService.getMyCart(authentication.getName());
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request,
                                Authentication authentication) {
        return cartService.addItem(authentication.getName(), request);
    }

    @PutMapping("/items/{id}")
    public CartResponse updateQty(@PathVariable Long id,
                                  @Valid @RequestBody UpdateCartItemRequest request,
                                  Authentication authentication) {
        return cartService.updateItemQuantity(authentication.getName(), id, request);
    }

    @DeleteMapping("/items/{id}")
    public CartResponse deleteItem(@PathVariable Long id,
                                   Authentication authentication) {
        return cartService.deleteItem(authentication.getName(), id);
    }
}
