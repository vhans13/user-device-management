package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.CreateDeviceRequest;
import com.example.devicemanagement.dto.DeviceResponse;
import com.example.devicemanagement.dto.UpdateDeviceRequest;
import com.example.devicemanagement.exception.GlobalExceptionHandler;
import com.example.devicemanagement.exception.ResourceNotFoundException;
import com.example.devicemanagement.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
@Import(GlobalExceptionHandler.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeviceService deviceService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void addDevice_shouldReturn201WithLocation() throws Exception {
        UUID deviceId = UUID.randomUUID();
        CreateDeviceRequest request = new CreateDeviceRequest("iPhone 15", "A2846");
        DeviceResponse response = new DeviceResponse(deviceId, "iPhone 15", "A2846",
                Instant.now(), Instant.now());

        when(deviceService.addDevice(eq(userId), any(CreateDeviceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/{userId}/devices", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/" + userId + "/devices/" + deviceId))
                .andExpect(jsonPath("$.deviceName").value("iPhone 15"))
                .andExpect(jsonPath("$.deviceModel").value("A2846"));
    }

    @Test
    void addDevice_withInvalidRequest_shouldReturn400() throws Exception {
        String invalidRequest = """
                { "deviceName": "", "deviceModel": "" }
                """;

        mockMvc.perform(post("/api/users/{userId}/devices", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void listDevices_shouldReturn200() throws Exception {
        DeviceResponse device = new DeviceResponse(UUID.randomUUID(), "iPhone 15", "A2846",
                Instant.now(), Instant.now());

        when(deviceService.listDevices(userId)).thenReturn(List.of(device));

        mockMvc.perform(get("/api/users/{userId}/devices", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName").value("iPhone 15"));
    }

    @Test
    void getDevice_whenNotFound_shouldReturn404() throws Exception {
        UUID deviceId = UUID.randomUUID();

        when(deviceService.getDevice(userId, deviceId)).thenThrow(
                new ResourceNotFoundException("Device with id '" + deviceId + "' not found"));

        mockMvc.perform(get("/api/users/{userId}/devices/{deviceId}", userId, deviceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void updateDevice_shouldReturn200() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UpdateDeviceRequest request = new UpdateDeviceRequest("iPad Pro 12.9", "A2378");
        DeviceResponse response = new DeviceResponse(deviceId, "iPad Pro 12.9", "A2378",
                Instant.now(), Instant.now());

        when(deviceService.updateDevice(eq(userId), eq(deviceId), any(UpdateDeviceRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/users/{userId}/devices/{deviceId}", userId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceName").value("iPad Pro 12.9"))
                .andExpect(jsonPath("$.deviceModel").value("A2378"));
    }

    @Test
    void deleteDevice_shouldReturn204() throws Exception {
        UUID deviceId = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{userId}/devices/{deviceId}", userId, deviceId))
                .andExpect(status().isNoContent());
    }
}
