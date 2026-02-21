package com.eventstream.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EventResponse {
    private String eventId;
    private String status;
    private Instant publishedAt;
}
