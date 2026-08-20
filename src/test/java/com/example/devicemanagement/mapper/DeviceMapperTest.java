package com.example.devicemanagement.mapper;

import com.example.devicemanagement.dto.DeviceResponse;
import com.example.devicemanagement.model.Device;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceMapperTest {

    private final DeviceMapper mapper = new DeviceMapper();

    @Test
    void toResponse_shouldMapAllFields() {
        UUID userId = UUID.randomUUID();
        Device device = new Device("iPhone 15", "A2846", userId);

        DeviceResponse response = mapper.toResponse(device);

        assertThat(response.id()).isEqualTo(device.getId());
        assertThat(response.deviceName()).isEqualTo("iPhone 15");
        assertThat(response.deviceModel()).isEqualTo("A2846");
        assertThat(response.createdAt()).isEqualTo(device.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(device.getUpdatedAt());
    }
}
