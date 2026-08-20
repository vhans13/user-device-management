package com.example.devicemanagement.dto;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String deviceName,
        String deviceModel,
        Instant createdAt,
        Instant updatedAt
) {
}
