package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.CreatePhoneNumberRequest;
import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.dto.SetPreferredPhoneNumberRequest;
import com.example.devicemanagement.service.PhoneNumberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}")
public class PhoneNumberController {

    private final PhoneNumberService phoneNumberService;

    public PhoneNumberController(PhoneNumberService phoneNumberService) {
        this.phoneNumberService = phoneNumberService;
    }

    @PostMapping("/phone-numbers")
    public ResponseEntity<PhoneNumberResponse> addPhoneNumber(
            @PathVariable UUID userId,
            @Valid @RequestBody CreatePhoneNumberRequest request) {
        PhoneNumberResponse phoneNumber = phoneNumberService.addPhoneNumber(userId, request);
        return ResponseEntity
                .created(URI.create("/api/users/" + userId + "/phone-numbers/" + phoneNumber.id()))
                .body(phoneNumber);
    }

    @GetMapping("/phone-numbers")
    public List<PhoneNumberResponse> listPhoneNumbers(@PathVariable UUID userId) {
        return phoneNumberService.listPhoneNumbers(userId);
    }

    @DeleteMapping("/phone-numbers/{phoneNumberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoneNumber(@PathVariable UUID userId, @PathVariable UUID phoneNumberId) {
        phoneNumberService.deletePhoneNumber(userId, phoneNumberId);
    }

    @PutMapping("/preferred-phone-number")
    public PhoneNumberResponse setPreferredPhoneNumber(
            @PathVariable UUID userId,
            @Valid @RequestBody SetPreferredPhoneNumberRequest request) {
        return phoneNumberService.setPreferredPhoneNumber(userId, request);
    }

    @GetMapping("/preferred-phone-number")
    public PhoneNumberResponse getPreferredPhoneNumber(@PathVariable UUID userId) {
        return phoneNumberService.getPreferredPhoneNumber(userId);
    }

    @DeleteMapping("/preferred-phone-number")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearPreferredPhoneNumber(@PathVariable UUID userId) {
        phoneNumberService.clearPreferredPhoneNumber(userId);
    }
}
