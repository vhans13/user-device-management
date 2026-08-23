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
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final UserMapper userMapper;
    private final PhoneNumberMapper phoneNumberMapper;
    private final DeviceService deviceService;
    private final PhoneNumberService phoneNumberService;

    public UserService(UserRepository userRepository,
                       PhoneNumberRepository phoneNumberRepository,
                       UserMapper userMapper,
                       PhoneNumberMapper phoneNumberMapper,
                       @Lazy DeviceService deviceService,
                       @Lazy PhoneNumberService phoneNumberService) {
        this.userRepository = userRepository;
        this.phoneNumberRepository = phoneNumberRepository;
        this.userMapper = userMapper;
        this.phoneNumberMapper = phoneNumberMapper;
        this.deviceService = deviceService;
        this.phoneNumberService = phoneNumberService;
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User with username '" + request.username() + "' already exists");
        }

        User user = new User(request.username(), request.email());
        user = userRepository.save(user);
        return userMapper.toResponse(user, null);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        // Batch-fetch all preferred phone numbers in one query (avoids N+1)
        List<UUID> phoneIds = users.getContent().stream()
                .map(User::getPreferredPhoneNumberId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, PhoneNumber> phonesById = phoneNumberRepository.findAllById(phoneIds).stream()
                .collect(Collectors.toMap(PhoneNumber::getId, Function.identity()));

        return users.map(user -> {
            PhoneNumber phone = phonesById.get(user.getPreferredPhoneNumberId());
            PhoneNumberResponse preferredPhone = phone != null ? phoneNumberMapper.toResponse(phone) : null;
            return userMapper.toResponse(user, preferredPhone);
        });
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = findUserOrThrow(userId);
        PhoneNumberResponse preferredPhone = resolvePreferredPhone(user);
        return userMapper.toResponse(user, preferredPhone);
    }

    public void deleteUser(UUID userId) {
        User user = findUserOrThrow(userId);

        deviceService.deleteAllForUser(userId);
        phoneNumberService.deleteAllForUser(userId);

        userRepository.delete(user);
    }

    public User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id '" + userId + "' not found"));
    }

    public void setPreferredPhoneNumberId(UUID userId, UUID phoneNumberId) {
        User user = findUserOrThrow(userId);
        user.setPreferredPhoneNumberId(phoneNumberId);
        userRepository.saveAndFlush(user);
    }

    public void clearPreferredPhoneNumberId(UUID userId) {
        User user = findUserOrThrow(userId);
        user.setPreferredPhoneNumberId(null);
        userRepository.saveAndFlush(user);
    }

    private PhoneNumberResponse resolvePreferredPhone(User user) {
        if (user.getPreferredPhoneNumberId() == null) {
            return null;
        }
        return phoneNumberRepository.findById(user.getPreferredPhoneNumberId())
                .map(phoneNumberMapper::toResponse)
                .orElse(null);
    }
}
