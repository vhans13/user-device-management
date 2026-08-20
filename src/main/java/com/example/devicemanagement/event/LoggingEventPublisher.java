package com.example.devicemanagement.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publishDeviceEvent(DeviceEvent event) {
        log.info("[DEV] Device event: type={}, userId={}, resourceId={}, payload={}",
                event.eventType(), event.userId(), event.resourceId(), event.payload());
    }

    @Override
    public void publishPhoneNumberEvent(PhoneNumberEvent event) {
        log.info("[DEV] Phone number event: type={}, userId={}, resourceId={}, payload={}",
                event.eventType(), event.userId(), event.resourceId(), event.payload());
    }
}
