package com.elemental.backend.service;

import com.elemental.backend.dto.AuthRequest;
import com.elemental.backend.dto.AuthResponse;
import com.elemental.backend.dto.RegisterRequest;
import com.elemental.backend.entity.Role;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.ConflictException;
import com.elemental.backend.repository.UserRepository;
import com.elemental.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final StripeService stripeService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           StripeService stripeService) {
        this.userRepository      = userRepository;
        this.passwordEncoder     = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService          = jwtService;
        this.stripeService       = stripeService;
    }

    @Override
    public AuthResponse register(RegisterRequest request, boolean admin) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Este correo ya está registrado");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(admin ? Role.ROLE_ADMIN : Role.ROLE_USER);

        // Guardar primero para tener el ID
        userRepository.save(user);

        // Crear Customer en Stripe y guardar el ID
        try {
            String name = ((request.getFirstName() != null ? request.getFirstName() : "")
                    + " " + (request.getLastName() != null ? request.getLastName() : "")).trim();
            String customerId = stripeService.createCustomer(user.getEmail(), name);
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        } catch (Exception e) {
            System.err.println("Warning: No se pudo crear Stripe Customer: " + e.getMessage());
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    @Override
    public AuthResponse login(AuthRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}