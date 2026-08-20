package com.example.devicemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePhoneNumberRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g. +1234567890)")
        String number,

        @NotBlank(message = "Label is required")
        @Size(max = 30, message = "Label must not exceed 30 characters")
        String label
) {
}
