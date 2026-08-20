package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.CreatePhoneNumberRequest;
import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.dto.SetPreferredPhoneNumberRequest;
import com.example.devicemanagement.event.EventPublisher;
import com.example.devicemanagement.event.PhoneNumberEvent;
import com.example.devicemanagement.exception.DuplicateResourceException;
import com.example.devicemanagement.exception.ResourceNotFoundException;
import com.example.devicemanagement.mapper.PhoneNumberMapper;
import com.example.devicemanagement.model.PhoneNumber;
import com.example.devicemanagement.model.User;
import com.example.devicemanagement.repository.PhoneNumberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneNumberServiceTest {

    @Mock
    private PhoneNumberRepository phoneNumberRepository;

    @Mock
    private PhoneNumberMapper phoneNumberMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private UserService userService;

    @InjectMocks
    private PhoneNumberService phoneNumberService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User("evarhan", "varun.hans@gmail.com");
    }

    @Test
    void addPhoneNumber_shouldSaveAndPublishEvent() {
        // Given
        CreatePhoneNumberRequest request = new CreatePhoneNumberRequest("+353870933771", "work");
        PhoneNumber savedPhone = new PhoneNumber("+353870933771", "work", userId);

        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(phoneNumberRepository.existsByUserIdAndNumber(userId, "+353870933771")).thenReturn(false);
        when(phoneNumberRepository.saveAndFlush(any(PhoneNumber.class))).thenReturn(savedPhone);
        when(phoneNumberMapper.toResponse(any(PhoneNumber.class))).thenReturn(
                new PhoneNumberResponse(savedPhone.getId(), "+353870933771", "work", savedPhone.getCreatedAt()));

        // When
        PhoneNumberResponse response = phoneNumberService.addPhoneNumber(userId, request);

        // Then
        assertThat(response.number()).isEqualTo("+353870933771");
        assertThat(response.label()).isEqualTo("work");

        ArgumentCaptor<PhoneNumberEvent> eventCaptor = ArgumentCaptor.forClass(PhoneNumberEvent.class);
        verify(eventPublisher).publishPhoneNumberEvent(eventCaptor.capture());

        PhoneNumberEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.eventType()).isEqualTo(PhoneNumberEvent.PhoneNumberEventType.PHONE_NUMBER_ADDED);
        assertThat(publishedEvent.userId()).isEqualTo(userId);
        assertThat(publishedEvent.payload().number()).isEqualTo("+353870933771");
        assertThat(publishedEvent.payload().label()).isEqualTo("work");
    }

    @Test
    void addPhoneNumber_duplicate_shouldThrow409() {
        // Given
        CreatePhoneNumberRequest request = new CreatePhoneNumberRequest("+353870933771", "work");

        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(phoneNumberRepository.existsByUserIdAndNumber(userId, "+353870933771")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> phoneNumberService.addPhoneNumber(userId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("+353870933771");

        verify(phoneNumberRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishPhoneNumberEvent(any());
    }

    @Test
    void deletePhoneNumber_shouldClearPreferredIfMatches() {
        // Given
        UUID phoneNumberId = UUID.randomUUID();
        PhoneNumber phoneNumber = new PhoneNumber("+353870933771", "work", userId);

        // Simulate this phone being the preferred one
        user.setPreferredPhoneNumberId(phoneNumberId);

        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(phoneNumberRepository.findByIdAndUserId(phoneNumberId, userId)).thenReturn(Optional.of(phoneNumber));

        // When
        phoneNumberService.deletePhoneNumber(userId, phoneNumberId);

        // Then
        verify(userService).clearPreferredPhoneNumberId(userId);
        verify(phoneNumberRepository).delete(phoneNumber);
        verify(phoneNumberRepository).flush();
        verify(eventPublisher).publishPhoneNumberEvent(any(PhoneNumberEvent.class));
    }

    @Test
    void setPreferredPhoneNumber_shouldUpdateAndPublishEvent() {
        // Given
        UUID phoneNumberId = UUID.randomUUID();
        SetPreferredPhoneNumberRequest request = new SetPreferredPhoneNumberRequest(phoneNumberId);
        PhoneNumber phoneNumber = new PhoneNumber("+353870933771", "work", userId);

        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(phoneNumberRepository.findByIdAndUserId(phoneNumberId, userId)).thenReturn(Optional.of(phoneNumber));
        when(phoneNumberMapper.toResponse(any(PhoneNumber.class))).thenReturn(
                new PhoneNumberResponse(phoneNumberId, "+353870933771", "work", phoneNumber.getCreatedAt()));

        // When
        PhoneNumberResponse response = phoneNumberService.setPreferredPhoneNumber(userId, request);

        // Then
        assertThat(response.number()).isEqualTo("+353870933771");
        verify(userService).setPreferredPhoneNumberId(userId, phoneNumber.getId());

        ArgumentCaptor<PhoneNumberEvent> eventCaptor = ArgumentCaptor.forClass(PhoneNumberEvent.class);
        verify(eventPublisher).publishPhoneNumberEvent(eventCaptor.capture());

        PhoneNumberEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.eventType()).isEqualTo(PhoneNumberEvent.PhoneNumberEventType.PREFERRED_PHONE_NUMBER_CHANGED);
    }

    @Test
    void getPreferredPhoneNumber_whenNoneSet_shouldThrow() {
        // Given
        when(userService.findUserOrThrow(userId)).thenReturn(user);
        // user.getPreferredPhoneNumberId() is null by default

        // When/Then
        assertThatThrownBy(() -> phoneNumberService.getPreferredPhoneNumber(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No preferred phone number set");
    }
}
