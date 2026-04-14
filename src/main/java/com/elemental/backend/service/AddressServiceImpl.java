package com.elemental.backend.service;

import com.elemental.backend.dto.AddressRequest;
import com.elemental.backend.dto.AddressResponse;
import com.elemental.backend.entity.Address;
import com.elemental.backend.entity.User;
import com.elemental.backend.exception.NotFoundException;
import com.elemental.backend.repository.AddressRepository;
import com.elemental.backend.repository.OrderRepository;
import com.elemental.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository    userRepository;
    private final OrderRepository   orderRepository;

    public AddressServiceImpl(AddressRepository addressRepository,
                              UserRepository userRepository,
                              OrderRepository orderRepository) {
        this.addressRepository = addressRepository;
        this.userRepository    = userRepository;
        this.orderRepository   = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(String email) {
        return addressRepository.findByUserEmailOrderByUpdatedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AddressResponse createMyAddress(String email, AddressRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Address address = new Address();
        address.setUser(user);
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        Address saved = addressRepository.save(address);
        return toResponse(saved);
    }

    @Override
    public AddressResponse updateMyAddress(String email, Long addressId, AddressRequest request) {

        Address address = addressRepository.findByIdAndUserEmail(addressId, email)
                .orElseThrow(() -> new NotFoundException("Dirección no encontrada"));

        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        Address saved = addressRepository.save(address);
        return toResponse(saved);
    }

    @Override
    public void deleteMyAddress(String email, Long addressId) {
        Address address = addressRepository.findByIdAndUserEmail(addressId, email)
                .orElseThrow(() -> new NotFoundException("Dirección no encontrada"));

        orderRepository.nullifyAddress(address);
        addressRepository.delete(address);
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getStreet(),
                address.getCity(),
                address.getPostalCode(),
                address.getCountry(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
