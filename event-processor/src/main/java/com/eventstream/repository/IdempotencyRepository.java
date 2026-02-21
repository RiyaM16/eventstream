package com.eventstream.repository;

import com.eventstream.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    boolean existsByEventId(String eventId);
}
