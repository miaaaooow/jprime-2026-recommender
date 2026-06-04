package com.example.recommendation.api.controller;

import com.example.recommendation.api.dto.CreateUserRequest;
import com.example.recommendation.api.dto.UserResponse;
import com.example.recommendation.api.dto.UserProfileResponse;
import com.example.recommendation.profile.service.UserProfileService;
import com.example.recommendation.user.model.UserArticleInteractionType;
import com.example.recommendation.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    public UserController(UserService userService, UserProfileService userProfileService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createUser(request));
    }

    @GetMapping
    public java.util.List<UserResponse> getUsers() {
        return userService.listUsers().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return UserResponse.from(userService.getUser(userId));
    }

    @PostMapping("/{userId}/reads/{articleId}")
    public UserProfileResponse readArticle(@PathVariable Long userId, @PathVariable String articleId) {
        return UserProfileResponse.from(userProfileService.recordInteraction(
                userId,
                articleId,
                UserArticleInteractionType.READ
        ));
    }

    @PostMapping("/{userId}/likes/{articleId}")
    public UserProfileResponse likeArticle(@PathVariable Long userId, @PathVariable String articleId) {
        return UserProfileResponse.from(userProfileService.recordInteraction(
                userId,
                articleId,
                UserArticleInteractionType.LIKE
        ));
    }

    @PostMapping("/{userId}/shares/{articleId}")
    public UserProfileResponse shareArticle(@PathVariable Long userId, @PathVariable String articleId) {
        return UserProfileResponse.from(userProfileService.recordInteraction(
                userId,
                articleId,
                UserArticleInteractionType.SHARE
        ));
    }

    @PostMapping("/{userId}/profile/rebuild")
    public UserProfileResponse rebuildProfile(@PathVariable Long userId) {
        return UserProfileResponse.from(userProfileService.rebuildProfile(userId));
    }

    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable Long userId) {
        return UserProfileResponse.from(userProfileService.getProfile(userId));
    }
}
