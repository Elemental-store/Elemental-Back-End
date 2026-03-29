package com.elemental.backend.service;

import com.elemental.backend.dto.AddressRequest;
import com.elemental.backend.dto.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getMyAddresses(String email);
    AddressResponse createMyAddress(String email, AddressRequest request);
    AddressResponse updateMyAddress(String email, Long addressId, AddressRequest request);
    void deleteMyAddress(String email, Long addressId);
}
