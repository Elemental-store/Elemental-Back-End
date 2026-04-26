package com.elemental.backend.service;

import com.elemental.backend.entity.User;
import com.elemental.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeCustomerService {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    public StripeCustomerService(StripeService stripeService, UserRepository userRepository) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
    }

    @Transactional
    public String ensureCustomer(User user) {
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
            return user.getStripeCustomerId();
        }

        String customerId = stripeService.createCustomer(user.getEmail(), fullName(user));
        user.setStripeCustomerId(customerId);
        userRepository.save(user);
        return customerId;
    }

    private String fullName(User user) {
        return ((user.getFirstName() != null ? user.getFirstName() : "")
                + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
    }
}
