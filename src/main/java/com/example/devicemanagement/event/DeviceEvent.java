package com.example.devicemanagement.event;

import java.time.Instant;
import java.util.UUID;

public record DeviceEvent(
        UUID eventId,
        DeviceEventType eventType,
        Instant timestamp,
        UUID userId,
        UUID resourceId,
        DevicePayload payload
) implements DomainEvent {
    public enum DeviceEventType {
        DEVICE_ADDED,
        DEVICE_UPDATED,
        DEVICE_REMOVED
    }

    public static DeviceEvent of(DeviceEventType type, UUID userId, UUID deviceId, DevicePayload payload) {
        return new DeviceEvent(
                UUID.randomUUID(),
                type,
                Instant.now(),
                userId,
                deviceId,
                payload
        );
    }
}
