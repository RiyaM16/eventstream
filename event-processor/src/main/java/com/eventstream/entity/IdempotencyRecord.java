package com.eventstream.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
