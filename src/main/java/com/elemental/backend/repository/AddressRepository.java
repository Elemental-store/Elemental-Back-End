package com.elemental.backend.repository;

import com.elemental.backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserEmailOrderByUpdatedAtDesc(String email);

    Optional<Address> findByIdAndUserEmail(Long id, String email);

}
