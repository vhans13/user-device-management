package com.example.devicemanagement.mapper;

import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.dto.UserResponse;
import com.example.devicemanagement.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toResponse_withPreferredPhone_shouldMapAllFields() {
        User user = new User("evarhan", "varun.hans@gmail.com");
        PhoneNumberResponse preferredPhone = new PhoneNumberResponse(
                UUID.randomUUID(), "+353870933771", "work", Instant.now());

        UserResponse response = mapper.toResponse(user, preferredPhone);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.username()).isEqualTo("evarhan");
        assertThat(response.email()).isEqualTo("varun.hans@gmail.com");
        assertThat(response.preferredPhoneNumber()).isEqualTo(preferredPhone);
        assertThat(response.createdAt()).isEqualTo(user.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(user.getUpdatedAt());
    }

    @Test
    void toResponse_withoutPreferredPhone_shouldMapWithNull() {
        User user = new User("evarhan", "varun.hans@gmail.com");

        UserResponse response = mapper.toResponse(user, null);

        assertThat(response.username()).isEqualTo("evarhan");
        assertThat(response.preferredPhoneNumber()).isNull();
    }
}
