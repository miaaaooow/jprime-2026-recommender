package com.example.recommendation.api.controller;

import com.example.recommendation.api.dto.AuthorResponse;
import com.example.recommendation.api.dto.CreateAuthorRequest;
import com.example.recommendation.author.service.AuthorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse createAuthor(@Valid @RequestBody CreateAuthorRequest request) {
        return authorService.createAuthor(request);
    }

    @GetMapping("/{authorId}")
    public AuthorResponse getAuthor(@PathVariable Long authorId) {
        return authorService.getAuthor(authorId);
    }

    @GetMapping
    public List<AuthorResponse> getAuthors() {
        return authorService.getAllAuthors();
    }
}
