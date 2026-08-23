package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.CreateDeviceRequest;
import com.example.devicemanagement.dto.DeviceResponse;
import com.example.devicemanagement.dto.UpdateDeviceRequest;
import com.example.devicemanagement.event.DeviceEvent;
import com.example.devicemanagement.event.DevicePayload;
import com.example.devicemanagement.event.EventPublisher;
import com.example.devicemanagement.exception.ResourceNotFoundException;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.model.Device;
import com.example.devicemanagement.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;
    private final EventPublisher eventPublisher;
    private final UserService userService;

    public DeviceService(DeviceRepository deviceRepository,
                         DeviceMapper deviceMapper,
                         EventPublisher eventPublisher,
                         UserService userService) {
        this.deviceRepository = deviceRepository;
        this.deviceMapper = deviceMapper;
        this.eventPublisher = eventPublisher;
        this.userService = userService;
    }

    public DeviceResponse addDevice(UUID userId, CreateDeviceRequest request) {
        userService.findUserOrThrow(userId);

        Device device = new Device(request.deviceName(), request.deviceModel(), userId);
        device = deviceRepository.saveAndFlush(device);

        DeviceEvent event = DeviceEvent.of(
                DeviceEvent.DeviceEventType.DEVICE_ADDED,
                userId,
                device.getId(),
                new DevicePayload(device.getDeviceName(), device.getDeviceModel())
        );
        eventPublisher.publishDeviceEvent(event);

        return deviceMapper.toResponse(device);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> listDevices(UUID userId) {
        userService.findUserOrThrow(userId);
        return deviceRepository.findByUserId(userId).stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(UUID userId, UUID deviceId) {
        userService.findUserOrThrow(userId);
        Device device = findDeviceOrThrow(userId, deviceId);
        return deviceMapper.toResponse(device);
    }

    public DeviceResponse updateDevice(UUID userId, UUID deviceId, UpdateDeviceRequest request) {
        userService.findUserOrThrow(userId);
        Device device = findDeviceOrThrow(userId, deviceId);

        device.setDeviceName(request.deviceName());
        device.setDeviceModel(request.deviceModel());
        device = deviceRepository.saveAndFlush(device);

        DeviceEvent event = DeviceEvent.of(
                DeviceEvent.DeviceEventType.DEVICE_UPDATED,
                userId,
                device.getId(),
                new DevicePayload(device.getDeviceName(), device.getDeviceModel())
        );
        eventPublisher.publishDeviceEvent(event);

        return deviceMapper.toResponse(device);
    }

    public void deleteDevice(UUID userId, UUID deviceId) {
        userService.findUserOrThrow(userId);
        Device device = findDeviceOrThrow(userId, deviceId);

        deviceRepository.delete(device);
        deviceRepository.flush();

        DeviceEvent event = DeviceEvent.of(
                DeviceEvent.DeviceEventType.DEVICE_REMOVED,
                userId,
                device.getId(),
                new DevicePayload(device.getDeviceName(), device.getDeviceModel())
        );
        eventPublisher.publishDeviceEvent(event);
    }

    public void deleteAllForUser(UUID userId) {
        List<Device> devices = deviceRepository.findByUserId(userId);
        deviceRepository.deleteAll(devices);
        deviceRepository.flush();

        devices.forEach(device -> {
            DeviceEvent event = DeviceEvent.of(
                    DeviceEvent.DeviceEventType.DEVICE_REMOVED,
                    userId,
                    device.getId(),
                    new DevicePayload(device.getDeviceName(), device.getDeviceModel())
            );
            eventPublisher.publishDeviceEvent(event);
        });
    }

    private Device findDeviceOrThrow(UUID userId, UUID deviceId) {
        return deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device with id '" + deviceId + "' not found"));
    }
}
