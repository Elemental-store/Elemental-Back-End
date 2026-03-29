package com.elemental.backend.service;

import com.elemental.backend.dto.*;

public interface CartService {
    CartResponse getMyCart(String email);
    CartResponse addItem(String email, AddCartItemRequest request);
    CartResponse updateItemQuantity(String email, Long itemId, UpdateCartItemRequest request);
    CartResponse deleteItem(String email, Long itemId);
}
