package com.eventstream.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "events")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Event {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "source", nullable = false)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "correlation_id")
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;

    @Column(name = "event_timestamp")
    private Instant eventTimestamp;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = Instant.now(); }

    public enum EventStatus { RECEIVED, PROCESSING, PROCESSED, FAILED }
}
