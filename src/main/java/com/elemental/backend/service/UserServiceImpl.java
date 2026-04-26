package com.elemental.backend.service;

import com.elemental.backend.dto.AdminUserUpdateRequest;
import com.elemental.backend.dto.UserProfileResponse;
import com.elemental.backend.dto.UserProfileUpdateRequest;
import com.elemental.backend.entity.Address;
import com.elemental.backend.entity.Role;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.AddressRepository;
import com.elemental.backend.repository.CartRepository;
import com.elemental.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;

    public UserServiceImpl(UserRepository userRepository,
                           CartRepository cartRepository,
                           AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Usuario desactivado");
        }

        return toResponse(user);
    }

    @Override
    public UserProfileResponse updateMyProfile(String email, UserProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Usuario desactivado");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());

        return toResponse(userRepository.saveAndFlush(user));
    }

    @Override
    public void deleteMyAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        cartRepository.findByUserEmailWithItems(email).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });

        List<Address> addresses = addressRepository.findByUserEmailOrderByUpdatedAtDesc(email);
        for (Address a : addresses) {
            a.setStreet("DELETED");
            a.setCity("DELETED");
            a.setPostalCode("DELETED");
            a.setCountry("DELETED");
        }
        addressRepository.saveAll(addresses);

        user.setFirstName(null);
        user.setLastName(null);
        user.setPhone(null);

        user.setPasswordHash("{bcrypt}$2a$10$FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");

        user.setEnabled(false);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.saveAndFlush(user);
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getFirstName(),
                user.getLastName(),
                user.getPhone()
        );
    }

    @Override
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UserProfileResponse updateUserById(Long id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRole() != null) user.setRole(Role.valueOf(request.getRole()));

        userRepository.save(user);

        return toResponse(user);
    }

    @Override
    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }
}
