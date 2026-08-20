package com.example.devicemanagement.event;

import java.util.UUID;

public sealed interface DomainEvent permits DeviceEvent, PhoneNumberEvent {
    UUID userId();
}
