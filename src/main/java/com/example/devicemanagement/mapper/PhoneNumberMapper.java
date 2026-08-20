package com.example.devicemanagement.mapper;

import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.model.PhoneNumber;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberMapper {

    public PhoneNumberResponse toResponse(PhoneNumber phoneNumber) {
        return new PhoneNumberResponse(
                phoneNumber.getId(),
                phoneNumber.getNumber(),
                phoneNumber.getLabel(),
                phoneNumber.getCreatedAt()
        );
    }
}
