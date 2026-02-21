package com.eventstream.controller;

import com.eventstream.dto.EventMessage;
import com.eventstream.dto.EventRequest;
import com.eventstream.dto.EventResponse;
import com.eventstream.publisher.EventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EventController {

    private final EventPublisher eventPublisher;

    @PostMapping("/events")
    public ResponseEntity<EventResponse> publish(@Valid @RequestBody EventRequest req) {
        String eventId = UUID.randomUUID().toString();
        log.info("Received event request [{}] type [{}]", eventId, req.getEventType());

        EventMessage msg = EventMessage.builder()
                .eventId(eventId)
                .eventType(req.getEventType())
                .source(req.getSource())
                .payload(req.getPayload())
                .correlationId(req.getCorrelationId() != null
                        ? req.getCorrelationId() : UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build();

        eventPublisher.publish(msg);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                EventResponse.builder()
                        .eventId(eventId)
                        .status("PUBLISHED")
                        .publishedAt(Instant.now())
                        .build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
}
