package com.example.devicemanagement.mapper;

import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.model.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberMapperTest {

    private final PhoneNumberMapper mapper = new PhoneNumberMapper();

    @Test
    void toResponse_shouldMapAllFields() {
        UUID userId = UUID.randomUUID();
        PhoneNumber phone = new PhoneNumber("+353870933771", "work", userId);

        PhoneNumberResponse response = mapper.toResponse(phone);

        assertThat(response.id()).isEqualTo(phone.getId());
        assertThat(response.number()).isEqualTo("+353870933771");
        assertThat(response.label()).isEqualTo("work");
        assertThat(response.createdAt()).isEqualTo(phone.getCreatedAt());
    }
}
