package com.example.devicemanagement.mapper;

import com.example.devicemanagement.dto.DeviceResponse;
import com.example.devicemanagement.model.Device;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getDeviceName(),
                device.getDeviceModel(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
