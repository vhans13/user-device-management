package com.example.devicemanagement.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SetPreferredPhoneNumberRequest(
        @NotNull(message = "Phone number ID is required")
        UUID phoneNumberId
) {
}
