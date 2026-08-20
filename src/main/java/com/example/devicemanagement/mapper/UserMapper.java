package com.example.devicemanagement.mapper;

import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.dto.UserResponse;
import com.example.devicemanagement.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user, PhoneNumberResponse preferredPhone) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                preferredPhone,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
