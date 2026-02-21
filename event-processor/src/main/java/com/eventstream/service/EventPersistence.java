package com.eventstream.service;

import com.eventstream.dto.EventMessage;
import com.eventstream.dto.NotificationMessage;
import com.eventstream.entity.Event;
import com.eventstream.entity.IdempotencyRecord;
import com.eventstream.repository.EventRepository;
import com.eventstream.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Separate bean so @Transactional goes through the Spring proxy.
 * Calling persistEvent() from EventProcessorService injects THIS bean,
 * so the call goes through the proxy — @Transactional is honoured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPersistence {

    private final EventRepository eventRepository;
    private final IdempotencyRepository idempotencyRepository;

    @Transactional
    public NotificationMessage persistEvent(EventMessage msg) {
        log.info("Processing event [{}] type [{}]", msg.getEventId(), msg.getEventType());

        // Idempotency check — if already processed, skip everything
        if (idempotencyRepository.existsByEventId(msg.getEventId())) {
            log.warn("Event [{}] already processed — skipping duplicate", msg.getEventId());
            return null;
        }

        // Save with PROCESSING status
        Event event = Event.builder()
                .eventId(msg.getEventId())
                .eventType(msg.getEventType())
                .source(msg.getSource())
                .payload(msg.getPayload())
                .correlationId(msg.getCorrelationId())
                .status(Event.EventStatus.PROCESSING)
                .eventTimestamp(msg.getTimestamp())
                .build();
        eventRepository.save(event);

        // Update to PROCESSED
        event.setStatus(Event.EventStatus.PROCESSED);
        event.setProcessedAt(Instant.now());
        eventRepository.save(event);

        // Record idempotency key — inside same transaction as event saves
        idempotencyRepository.save(IdempotencyRecord.builder()
                .eventId(msg.getEventId())
                .processedAt(Instant.now())
                .build());

        log.info("Event [{}] saved as PROCESSED — transaction will commit on return", msg.getEventId());

        // Return the notification to publish — caller publishes AFTER this method
        // returns, meaning AFTER the transaction commits
        return NotificationMessage.builder()
                .notificationId(UUID.randomUUID().toString())
                .eventId(msg.getEventId())
                .eventType(msg.getEventType())
                .correlationId(msg.getCorrelationId())
                .metadata(Map.of("source", msg.getSource()))
                .createdAt(Instant.now())
                .build();
    }
}
