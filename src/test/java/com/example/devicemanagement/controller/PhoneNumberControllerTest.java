package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.CreatePhoneNumberRequest;
import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.dto.SetPreferredPhoneNumberRequest;
import com.example.devicemanagement.exception.GlobalExceptionHandler;
import com.example.devicemanagement.exception.ResourceNotFoundException;
import com.example.devicemanagement.service.PhoneNumberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PhoneNumberController.class)
@Import(GlobalExceptionHandler.class)
class PhoneNumberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PhoneNumberService phoneNumberService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void addPhoneNumber_shouldReturn201WithLocation() throws Exception {
        UUID phoneId = UUID.randomUUID();
        CreatePhoneNumberRequest request = new CreatePhoneNumberRequest("+353870933771", "work");
        PhoneNumberResponse response = new PhoneNumberResponse(phoneId, "+353870933771", "work", Instant.now());

        when(phoneNumberService.addPhoneNumber(eq(userId), any(CreatePhoneNumberRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/users/{userId}/phone-numbers", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/" + userId + "/phone-numbers/" + phoneId))
                .andExpect(jsonPath("$.number").value("+353870933771"))
                .andExpect(jsonPath("$.label").value("work"));
    }

    @Test
    void addPhoneNumber_withInvalidFormat_shouldReturn400() throws Exception {
        String invalidRequest = """
                { "number": "123", "label": "" }
                """;

        mockMvc.perform(post("/api/users/{userId}/phone-numbers", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void listPhoneNumbers_shouldReturn200() throws Exception {
        PhoneNumberResponse phone = new PhoneNumberResponse(UUID.randomUUID(), "+353870933771", "work", Instant.now());

        when(phoneNumberService.listPhoneNumbers(userId)).thenReturn(List.of(phone));

        mockMvc.perform(get("/api/users/{userId}/phone-numbers", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("+353870933771"));
    }

    @Test
    void setPreferredPhoneNumber_shouldReturn200() throws Exception {
        UUID phoneId = UUID.randomUUID();
        SetPreferredPhoneNumberRequest request = new SetPreferredPhoneNumberRequest(phoneId);
        PhoneNumberResponse response = new PhoneNumberResponse(phoneId, "+353870933771", "work", Instant.now());

        when(phoneNumberService.setPreferredPhoneNumber(eq(userId), any(SetPreferredPhoneNumberRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/users/{userId}/preferred-phone-number", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("+353870933771"));
    }

    @Test
    void getPreferredPhoneNumber_whenNoneSet_shouldReturn404() throws Exception {
        when(phoneNumberService.getPreferredPhoneNumber(userId)).thenThrow(
                new ResourceNotFoundException("No preferred phone number set for user '" + userId + "'"));

        mockMvc.perform(get("/api/users/{userId}/preferred-phone-number", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void deletePreferredPhoneNumber_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}/preferred-phone-number", userId))
                .andExpect(status().isNoContent());
    }
}
