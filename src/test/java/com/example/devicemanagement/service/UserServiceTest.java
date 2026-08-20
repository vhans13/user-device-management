package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.CreateUserRequest;
import com.example.devicemanagement.dto.PhoneNumberResponse;
import com.example.devicemanagement.dto.UserResponse;
import com.example.devicemanagement.exception.DuplicateResourceException;
import com.example.devicemanagement.exception.ResourceNotFoundException;
import com.example.devicemanagement.mapper.PhoneNumberMapper;
import com.example.devicemanagement.mapper.UserMapper;
import com.example.devicemanagement.model.PhoneNumber;
import com.example.devicemanagement.model.User;
import com.example.devicemanagement.repository.PhoneNumberRepository;
import com.example.devicemanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhoneNumberRepository phoneNumberRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PhoneNumberMapper phoneNumberMapper;

    @Mock
    private DeviceService deviceService;

    @Mock
    private PhoneNumberService phoneNumberService;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldSaveAndReturnResponse() {
        // Given
        CreateUserRequest request = new CreateUserRequest("evarhan", "varun.hans@gmail.com");
        User savedUser = new User("evarhan", "varun.hans@gmail.com");
        UserResponse expectedResponse = new UserResponse(
                savedUser.getId(), "evarhan", "varun.hans@gmail.com", null,
                savedUser.getCreatedAt(), savedUser.getUpdatedAt());

        when(userRepository.existsByUsername("evarhan")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(any(User.class), isNull())).thenReturn(expectedResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertThat(response.username()).isEqualTo("evarhan");
        assertThat(response.email()).isEqualTo("varun.hans@gmail.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicate_shouldThrow409() {
        // Given
        CreateUserRequest request = new CreateUserRequest("evarhan", "varun.hans@gmail.com");
        when(userRepository.existsByUsername("evarhan")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("evarhan");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUser_shouldReturnResponse() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User("evarhan", "varun.hans@gmail.com");
        UserResponse expectedResponse = new UserResponse(
                userId, "evarhan", "varun.hans@gmail.com", null,
                user.getCreatedAt(), user.getUpdatedAt());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(any(User.class), isNull())).thenReturn(expectedResponse);

        // When
        UserResponse response = userService.getUser(userId);

        // Then
        assertThat(response.username()).isEqualTo("evarhan");
    }

    @Test
    void getUser_notFound_shouldThrow() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void listUsers_shouldReturnPaginatedResults() {
        // Given
        User user = new User("evarhan", "varun.hans@gmail.com");
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        UserResponse expectedResponse = new UserResponse(
                user.getId(), "evarhan", "varun.hans@gmail.com", null,
                user.getCreatedAt(), user.getUpdatedAt());

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(any(User.class), isNull())).thenReturn(expectedResponse);

        // When
        Page<UserResponse> result = userService.listUsers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("evarhan");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deleteUser_shouldDelegateCleanupAndDelete() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User("evarhan", "varun.hans@gmail.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.deleteUser(userId);

        // Then
        verify(deviceService).deleteAllForUser(userId);
        verify(phoneNumberService).deleteAllForUser(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void listUsers_withPreferredPhone_shouldBatchFetch() {
        // Given
        PhoneNumber phone = new PhoneNumber("+353870933771", "work", UUID.randomUUID());
        UUID phoneId = phone.getId(); // use the phone's actual generated ID

        User user = new User("evarhan", "varun.hans@gmail.com");
        user.setPreferredPhoneNumberId(phoneId);

        PhoneNumberResponse phoneResponse = new PhoneNumberResponse(phoneId, "+353870933771", "work", phone.getCreatedAt());

        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        UserResponse expectedResponse = new UserResponse(
                user.getId(), "evarhan", "varun.hans@gmail.com", phoneResponse,
                user.getCreatedAt(), user.getUpdatedAt());

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(phoneNumberRepository.findAllById(List.of(phoneId))).thenReturn(List.of(phone));
        when(phoneNumberMapper.toResponse(phone)).thenReturn(phoneResponse);
        when(userMapper.toResponse(eq(user), eq(phoneResponse))).thenReturn(expectedResponse);

        // When
        Page<UserResponse> result = userService.listUsers(pageable);

        // Then
        assertThat(result.getContent().get(0).preferredPhoneNumber()).isNotNull();
        assertThat(result.getContent().get(0).preferredPhoneNumber().number()).isEqualTo("+353870933771");
        verify(phoneNumberRepository).findAllById(List.of(phoneId));
    }
}
