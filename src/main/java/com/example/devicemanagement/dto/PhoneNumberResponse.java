package com.example.devicemanagement.dto;

import java.time.Instant;
import java.util.UUID;

public record PhoneNumberResponse(
        UUID id,
        String number,
        String label,
        Instant createdAt
) {
}
