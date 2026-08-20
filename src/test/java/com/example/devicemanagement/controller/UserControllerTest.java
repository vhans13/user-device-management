package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.CreateUserRequest;
import com.example.devicemanagement.dto.UserResponse;
import com.example.devicemanagement.exception.GlobalExceptionHandler;
import com.example.devicemanagement.exception.ResourceNotFoundException;
import com.example.devicemanagement.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void createUser_shouldReturn201WithLocation() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest("evarhan", "varun.hans@gmail.com");
        UserResponse response = new UserResponse(userId, "evarhan", "varun.hans@gmail.com",
                null, Instant.now(), Instant.now());

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/" + userId))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("evarhan"))
                .andExpect(jsonPath("$.email").value("varun.hans@gmail.com"))
                .andExpect(jsonPath("$.preferredPhoneNumber").isEmpty());
    }

    @Test
    void createUser_withInvalidRequest_shouldReturn400() throws Exception {
        String invalidRequest = """
                { "username": "", "email": "not-an-email" }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void listUsers_shouldReturnPaginatedResults() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse user = new UserResponse(userId, "evarhan", "varun.hans@gmail.com",
                null, Instant.now(), Instant.now());
        Page<UserResponse> page = new PageImpl<>(List.of(user),
                org.springframework.data.domain.PageRequest.of(0, 20), 1);

        when(userService.listUsers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("evarhan"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getUser_whenNotFound_shouldReturn404() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.getUser(userId)).thenThrow(
                new ResourceNotFoundException("User with id '" + userId + "' not found"));

        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("User with id '" + userId + "' not found"));
    }
}
