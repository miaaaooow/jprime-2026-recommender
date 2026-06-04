package com.example.recommendation.user.service;

import com.example.recommendation.api.dto.CreateUserRequest;
import com.example.recommendation.common.exception.ResourceNotFoundException;
import com.example.recommendation.user.model.UserEntity;
import com.example.recommendation.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity createUser(CreateUserRequest request) {
        UserEntity user = new UserEntity(request.username().trim(), request.email().trim().toLowerCase());
        return userRepository.save(user);
    }

    public UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User %d was not found.".formatted(userId)));
    }
}
