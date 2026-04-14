package com.elemental.backend.service;

import com.elemental.backend.entity.PasswordResetToken;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.PasswordResetTokenRepository;
import com.elemental.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository  = userRepository;
        this.emailService    = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendResetEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Borramos tokens anteriores del mismo email
            tokenRepository.deleteByEmail(email);

            PasswordResetToken token = new PasswordResetToken();
            token.setToken(UUID.randomUUID().toString());
            token.setEmail(email);
            token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            tokenRepository.save(token);

            String link = "http://localhost:4200/reset-password?token=" + token.getToken();
            emailService.sendPasswordResetEmail(email, user.getFirstName(), link);
        });
    }

    @Transactional
    public void resetPassword(String tokenStr, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new NotFoundException("Token inválido"));

        if (token.isUsed()) throw new NotFoundException("Token ya utilizado");
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) throw new NotFoundException("Token expirado");

        User user = userRepository.findByEmail(token.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }
}