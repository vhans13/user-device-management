package com.example.devicemanagement.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        PhoneNumberResponse preferredPhoneNumber,
        Instant createdAt,
        Instant updatedAt
) {
}
