package com.eventstream.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationMessage {
    private String notificationId;
    private String eventId;
    private String eventType;
    private String correlationId;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
