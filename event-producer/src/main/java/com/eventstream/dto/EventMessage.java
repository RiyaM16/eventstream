package com.eventstream.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {
    private String eventId;
    private String eventType;
    private String source;
    private Map<String, Object> payload;
    private String correlationId;
    private Instant timestamp;
}
