package com.example.recommendation.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.recommendation.user.model.UserEntity;
import com.example.recommendation.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void listUsersOrdersByUsernameAscending() {
        UserService userService = new UserService(userRepository);
        List<UserEntity> users = List.of(
                new UserEntity("alice", "alice@example.com"),
                new UserEntity("bob", "bob@example.com")
        );
        Sort sort = Sort.by(Sort.Direction.ASC, "username");

        when(userRepository.findAll(sort)).thenReturn(users);

        assertThat(userService.listUsers()).containsExactlyElementsOf(users);
        verify(userRepository).findAll(sort);
    }
}
