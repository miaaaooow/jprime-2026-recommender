package com.example.recommendation.api.controller;

import com.example.recommendation.api.dto.CreatePublisherRequest;
import com.example.recommendation.api.dto.PublisherResponse;
import com.example.recommendation.publisher.service.PublisherService;
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
@RequestMapping("/api/publishers")
public class PublisherController {

    private final PublisherService publisherService;

    public PublisherController(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublisherResponse createPublisher(@Valid @RequestBody CreatePublisherRequest request) {
        return publisherService.createPublisher(request);
    }

    @GetMapping("/{publisherId}")
    public PublisherResponse getPublisher(@PathVariable Long publisherId) {
        return publisherService.getPublisher(publisherId);
    }

    @GetMapping
    public List<PublisherResponse> getPublishers() {
        return publisherService.getAllPublishers();
    }
}
