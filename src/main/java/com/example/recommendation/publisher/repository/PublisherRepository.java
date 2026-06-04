package com.example.recommendation.publisher.repository;

import com.example.recommendation.publisher.model.PublisherEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<PublisherEntity, Long> {

    Optional<PublisherEntity> findByNameIgnoreCase(String name);

    List<PublisherEntity> findAllByOrderByNameAsc();
}
