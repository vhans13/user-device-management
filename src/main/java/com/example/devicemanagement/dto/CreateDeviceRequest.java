package com.example.devicemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeviceRequest(
        @NotBlank(message = "Device name is required")
        @Size(max = 100, message = "Device name must not exceed 100 characters")
        String deviceName,

        @NotBlank(message = "Device model is required")
        @Size(max = 50, message = "Device model must not exceed 50 characters")
        String deviceModel
) {
}
