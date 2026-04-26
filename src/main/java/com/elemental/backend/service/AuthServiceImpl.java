package com.elemental.backend.service;

import com.elemental.backend.dto.AuthRequest;
import com.elemental.backend.dto.AuthResponse;
import com.elemental.backend.dto.RegisterRequest;
import com.elemental.backend.entity.Role;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.ConflictException;
import com.elemental.backend.repository.UserRepository;
import com.elemental.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final StripeCustomerService stripeCustomerService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           StripeCustomerService stripeCustomerService) {
        this.userRepository      = userRepository;
        this.passwordEncoder     = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService          = jwtService;
        this.stripeCustomerService = stripeCustomerService;
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

        userRepository.save(user);

        try {
            stripeCustomerService.ensureCustomer(user);
        } catch (Exception e) {
            log.warn("No se pudo crear el cliente de Stripe para {}", user.getEmail(), e);
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
