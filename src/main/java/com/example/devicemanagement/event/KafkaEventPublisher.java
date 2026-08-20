package com.example.devicemanagement.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String deviceEventsTopic;
    private final String phoneNumberEventsTopic;

    public KafkaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.device-events}") String deviceEventsTopic,
            @Value("${app.kafka.topics.phone-number-events}") String phoneNumberEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.deviceEventsTopic = deviceEventsTopic;
        this.phoneNumberEventsTopic = phoneNumberEventsTopic;
    }

    @Override
    public void publishDeviceEvent(DeviceEvent event) {
        publish(deviceEventsTopic, event);
    }

    @Override
    public void publishPhoneNumberEvent(PhoneNumberEvent event) {
        publish(phoneNumberEventsTopic, event);
    }

    private void publish(String topic, DomainEvent event) {
        String partitionKey = event.userId().toString();
        log.info("Publishing event to {}: {}", topic, event);

        kafkaTemplate.send(topic, partitionKey, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to {}: {}", topic, event, ex);
                    }
                });
    }
}
