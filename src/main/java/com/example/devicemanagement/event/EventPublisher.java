package com.example.devicemanagement.event;

public interface EventPublisher {

    void publishDeviceEvent(DeviceEvent event);

    void publishPhoneNumberEvent(PhoneNumberEvent event);
}
