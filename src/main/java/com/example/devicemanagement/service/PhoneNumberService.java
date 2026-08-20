package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.CreatePhoneNumberRequest;
import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.dto.SetPreferredPhoneNumberRequest;
import com.example.devicemanagement.event.EventPublisher;
import com.example.devicemanagement.event.PhoneNumberEvent;
import com.example.devicemanagement.event.PhoneNumberPayload;
import com.example.devicemanagement.exception.DuplicateResourceException;
import com.example.devicemanagement.exception.ResourceNotFoundException;
import com.example.devicemanagement.mapper.PhoneNumberMapper;
import com.example.devicemanagement.model.PhoneNumber;
import com.example.devicemanagement.model.User;
import com.example.devicemanagement.repository.PhoneNumberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PhoneNumberService {

    private final PhoneNumberRepository phoneNumberRepository;
    private final PhoneNumberMapper phoneNumberMapper;
    private final EventPublisher eventPublisher;
    private final UserService userService;

    public PhoneNumberService(PhoneNumberRepository phoneNumberRepository,
                              PhoneNumberMapper phoneNumberMapper,
                              EventPublisher eventPublisher,
                              UserService userService) {
        this.phoneNumberRepository = phoneNumberRepository;
        this.phoneNumberMapper = phoneNumberMapper;
        this.eventPublisher = eventPublisher;
        this.userService = userService;
    }

    public PhoneNumberResponse addPhoneNumber(UUID userId, CreatePhoneNumberRequest request) {
        userService.findUserOrThrow(userId);

        if (phoneNumberRepository.existsByUserIdAndNumber(userId, request.number())) {
            throw new DuplicateResourceException(
                    "Phone number '" + request.number() + "' already exists for this user");
        }

        PhoneNumber phoneNumber = new PhoneNumber(request.number(), request.label(), userId);
        phoneNumber = phoneNumberRepository.saveAndFlush(phoneNumber);

        PhoneNumberEvent event = PhoneNumberEvent.of(
                PhoneNumberEvent.PhoneNumberEventType.PHONE_NUMBER_ADDED,
                userId,
                phoneNumber.getId(),
                new PhoneNumberPayload(phoneNumber.getNumber(), phoneNumber.getLabel())
        );
        eventPublisher.publishPhoneNumberEvent(event);

        return phoneNumberMapper.toResponse(phoneNumber);
    }

    @Transactional(readOnly = true)
    public List<PhoneNumberResponse> listPhoneNumbers(UUID userId) {
        userService.findUserOrThrow(userId);
        return phoneNumberRepository.findByUserId(userId).stream()
                .map(phoneNumberMapper::toResponse)
                .toList();
    }

    public void deletePhoneNumber(UUID userId, UUID phoneNumberId) {
        User user = userService.findUserOrThrow(userId);
        PhoneNumber phoneNumber = findPhoneNumberOrThrow(userId, phoneNumberId);

        // Clear preferred phone number if this is the one being deleted
        if (phoneNumberId.equals(user.getPreferredPhoneNumberId())) {
            userService.clearPreferredPhoneNumberId(userId);
        }

        phoneNumberRepository.delete(phoneNumber);
        phoneNumberRepository.flush();

        PhoneNumberEvent event = PhoneNumberEvent.of(
                PhoneNumberEvent.PhoneNumberEventType.PHONE_NUMBER_REMOVED,
                userId,
                phoneNumber.getId(),
                new PhoneNumberPayload(phoneNumber.getNumber(), phoneNumber.getLabel())
        );
        eventPublisher.publishPhoneNumberEvent(event);
    }

    public PhoneNumberResponse setPreferredPhoneNumber(UUID userId, SetPreferredPhoneNumberRequest request) {
        userService.findUserOrThrow(userId);
        PhoneNumber phoneNumber = findPhoneNumberOrThrow(userId, request.phoneNumberId());

        userService.setPreferredPhoneNumberId(userId, phoneNumber.getId());

        PhoneNumberEvent event = PhoneNumberEvent.of(
                PhoneNumberEvent.PhoneNumberEventType.PREFERRED_PHONE_NUMBER_CHANGED,
                userId,
                phoneNumber.getId(),
                new PhoneNumberPayload(phoneNumber.getNumber(), phoneNumber.getLabel())
        );
        eventPublisher.publishPhoneNumberEvent(event);

        return phoneNumberMapper.toResponse(phoneNumber);
    }

    @Transactional(readOnly = true)
    public PhoneNumberResponse getPreferredPhoneNumber(UUID userId) {
        User user = userService.findUserOrThrow(userId);

        if (user.getPreferredPhoneNumberId() == null) {
            throw new ResourceNotFoundException("No preferred phone number set for user '" + userId + "'");
        }

        PhoneNumber phoneNumber = phoneNumberRepository.findById(user.getPreferredPhoneNumberId())
                .orElseThrow(() -> new ResourceNotFoundException("Preferred phone number not found"));

        return phoneNumberMapper.toResponse(phoneNumber);
    }

    public void clearPreferredPhoneNumber(UUID userId) {
        userService.clearPreferredPhoneNumberId(userId);
    }

    public void deleteAllForUser(UUID userId) {
        phoneNumberRepository.findByUserId(userId).forEach(phone -> {
            PhoneNumberEvent event = PhoneNumberEvent.of(
                    PhoneNumberEvent.PhoneNumberEventType.PHONE_NUMBER_REMOVED,
                    userId,
                    phone.getId(),
                    new PhoneNumberPayload(phone.getNumber(), phone.getLabel())
            );
            eventPublisher.publishPhoneNumberEvent(event);
            phoneNumberRepository.delete(phone);
        });
        phoneNumberRepository.flush();
    }

    private PhoneNumber findPhoneNumberOrThrow(UUID userId, UUID phoneNumberId) {
        return phoneNumberRepository.findByIdAndUserId(phoneNumberId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Phone number with id '" + phoneNumberId + "' not found"));
    }
}
