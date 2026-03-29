package com.elemental.backend.controller;

import com.elemental.backend.dto.AddressRequest;
import com.elemental.backend.dto.AddressResponse;
import com.elemental.backend.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my/addresses")
public class MyAddressController {

    private final AddressService addressService;

    public MyAddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> list(Authentication authentication) {
        return addressService.getMyAddresses(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@Valid @RequestBody AddressRequest request,
                                  Authentication authentication) {
        return addressService.createMyAddress(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id,
                                  @Valid @RequestBody AddressRequest request,
                                  Authentication authentication) {
        return addressService.updateMyAddress(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        addressService.deleteMyAddress(authentication.getName(), id);
    }
}
