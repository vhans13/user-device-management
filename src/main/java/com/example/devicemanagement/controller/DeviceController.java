package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.CreateDeviceRequest;
import com.example.devicemanagement.dto.DeviceResponse;
import com.example.devicemanagement.dto.UpdateDeviceRequest;
import com.example.devicemanagement.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> addDevice(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateDeviceRequest request) {
        DeviceResponse device = deviceService.addDevice(userId, request);
        return ResponseEntity
                .created(URI.create("/api/users/" + userId + "/devices/" + device.id()))
                .body(device);
    }

    @GetMapping
    public List<DeviceResponse> listDevices(@PathVariable UUID userId) {
        return deviceService.listDevices(userId);
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse getDevice(@PathVariable UUID userId, @PathVariable UUID deviceId) {
        return deviceService.getDevice(userId, deviceId);
    }

    @PutMapping("/{deviceId}")
    public DeviceResponse updateDevice(
            @PathVariable UUID userId,
            @PathVariable UUID deviceId,
            @Valid @RequestBody UpdateDeviceRequest request) {
        return deviceService.updateDevice(userId, deviceId, request);
    }

    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDevice(@PathVariable UUID userId, @PathVariable UUID deviceId) {
        deviceService.deleteDevice(userId, deviceId);
    }
}
