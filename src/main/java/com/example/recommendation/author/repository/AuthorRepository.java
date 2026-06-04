package com.example.recommendation.author.repository;

import com.example.recommendation.author.model.AuthorEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {

    List<AuthorEntity> findAllByOrderByNameAsc();

    List<AuthorEntity> findByPublisherIdOrderByNameAsc(Long publisherId);

    Optional<AuthorEntity> findByPublisherIdAndNameIgnoreCase(Long publisherId, String name);
}
