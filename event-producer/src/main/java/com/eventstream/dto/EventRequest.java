package com.eventstream.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class EventRequest {
    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotBlank(message = "source is required")
    private String source;

    @NotNull(message = "payload is required")
    private Map<String, Object> payload;

    private String correlationId;
}
