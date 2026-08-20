package com.example.devicemanagement.event;

import java.time.Instant;
import java.util.UUID;

public record PhoneNumberEvent(
        UUID eventId,
        PhoneNumberEventType eventType,
        Instant timestamp,
        UUID userId,
        UUID resourceId,
        PhoneNumberPayload payload
) implements DomainEvent {
    public enum PhoneNumberEventType {
        PHONE_NUMBER_ADDED,
        PHONE_NUMBER_REMOVED,
        PREFERRED_PHONE_NUMBER_CHANGED
    }

    public static PhoneNumberEvent of(PhoneNumberEventType type, UUID userId, UUID resourceId, PhoneNumberPayload payload) {
        return new PhoneNumberEvent(
                UUID.randomUUID(),
                type,
                Instant.now(),
                userId,
                resourceId,
                payload
        );
    }
}
