package com.example.devicemanagement.repository;

import com.example.devicemanagement.model.PhoneNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, UUID> {

    List<PhoneNumber> findByUserId(UUID userId);

    Optional<PhoneNumber> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNumber(UUID userId, String number);
}
