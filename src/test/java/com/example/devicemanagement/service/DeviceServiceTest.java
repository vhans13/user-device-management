package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.CreateDeviceRequest;
import com.example.devicemanagement.dto.DeviceResponse;
import com.example.devicemanagement.dto.UpdateDeviceRequest;
import com.example.devicemanagement.event.DeviceEvent;
import com.example.devicemanagement.event.EventPublisher;
import com.example.devicemanagement.exception.ResourceNotFoundException;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.model.Device;
import com.example.devicemanagement.model.User;
import com.example.devicemanagement.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private UserService userService;

    @InjectMocks
    private DeviceService deviceService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User("evarhan", "varun.hans@gmail.com");
    }

    @Test
    void addDevice_shouldSaveAndPublishEvent() {
        // Given
        CreateDeviceRequest request = new CreateDeviceRequest("iPhone 15", "A2846");
        Device savedDevice = new Device("iPhone 15", "A2846", userId);

        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(deviceRepository.saveAndFlush(any(Device.class))).thenReturn(savedDevice);
        when(deviceMapper.toResponse(any(Device.class))).thenReturn(
                new DeviceResponse(savedDevice.getId(), "iPhone 15", "A2846",
                        savedDevice.getCreatedAt(), savedDevice.getUpdatedAt()));

        // When
        DeviceResponse response = deviceService.addDevice(userId, request);

        // Then
        assertThat(response.deviceName()).isEqualTo("iPhone 15");
        assertThat(response.deviceModel()).isEqualTo("A2846");

        ArgumentCaptor<DeviceEvent> eventCaptor = ArgumentCaptor.forClass(DeviceEvent.class);
        verify(eventPublisher).publishDeviceEvent(eventCaptor.capture());

        DeviceEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.eventType()).isEqualTo(DeviceEvent.DeviceEventType.DEVICE_ADDED);
        assertThat(publishedEvent.userId()).isEqualTo(userId);
        assertThat(publishedEvent.payload().deviceName()).isEqualTo("iPhone 15");
        assertThat(publishedEvent.payload().deviceModel()).isEqualTo("A2846");
    }

    @Test
    void updateDevice_shouldUpdateAndPublishEvent() {
        // Given
        UUID deviceId = UUID.randomUUID();
        UpdateDeviceRequest request = new UpdateDeviceRequest("iPad Pro 12.9", "A2378");
        Device existingDevice = new Device("iPad Pro", "A2377", userId);

        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.saveAndFlush(any(Device.class))).thenReturn(existingDevice);
        when(deviceMapper.toResponse(any(Device.class))).thenReturn(
                new DeviceResponse(deviceId, "iPad Pro 12.9", "A2378",
                        existingDevice.getCreatedAt(), existingDevice.getUpdatedAt()));

        // When
        DeviceResponse response = deviceService.updateDevice(userId, deviceId, request);

        // Then
        assertThat(response.deviceName()).isEqualTo("iPad Pro 12.9");

        ArgumentCaptor<DeviceEvent> eventCaptor = ArgumentCaptor.forClass(DeviceEvent.class);
        verify(eventPublisher).publishDeviceEvent(eventCaptor.capture());

        DeviceEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.eventType()).isEqualTo(DeviceEvent.DeviceEventType.DEVICE_UPDATED);
    }

    @Test
    void deleteDevice_shouldDeleteAndPublishEvent() {
        // Given
        UUID deviceId = UUID.randomUUID();
        Device device = new Device("iPhone 15", "A2846", userId);

        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.of(device));

        // When
        deviceService.deleteDevice(userId, deviceId);

        // Then
        verify(deviceRepository).delete(device);

        ArgumentCaptor<DeviceEvent> eventCaptor = ArgumentCaptor.forClass(DeviceEvent.class);
        verify(eventPublisher).publishDeviceEvent(eventCaptor.capture());

        DeviceEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.eventType()).isEqualTo(DeviceEvent.DeviceEventType.DEVICE_REMOVED);
    }

    @Test
    void getDevice_whenNotFound_shouldThrow() {
        // Given
        UUID deviceId = UUID.randomUUID();
        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(deviceRepository.findByIdAndUserId(deviceId, userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviceService.getDevice(userId, deviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());
    }

    @Test
    void listDevices_shouldReturnMappedResponses() {
        // Given
        Device device1 = new Device("iPhone", "A2846", userId);
        Device device2 = new Device("iPad", "A2377", userId);

        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(deviceRepository.findByUserId(userId)).thenReturn(List.of(device1, device2));
        when(deviceMapper.toResponse(any(Device.class))).thenAnswer(invocation -> {
            Device d = invocation.getArgument(0);
            return new DeviceResponse(d.getId(), d.getDeviceName(), d.getDeviceModel(),
                    d.getCreatedAt(), d.getUpdatedAt());
        });

        // When
        List<DeviceResponse> responses = deviceService.listDevices(userId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).deviceName()).isEqualTo("iPhone");
        assertThat(responses.get(1).deviceName()).isEqualTo("iPad");
    }
}
