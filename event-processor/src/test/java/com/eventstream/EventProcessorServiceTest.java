package com.eventstream;

import com.eventstream.dto.EventMessage;
import com.eventstream.entity.Event;
import com.eventstream.entity.IdempotencyRecord;
import com.eventstream.publisher.NotificationPublisher;
import com.eventstream.repository.EventRepository;
import com.eventstream.repository.IdempotencyRepository;
import com.eventstream.service.EventProcessorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProcessorServiceTest {

    @Mock EventRepository eventRepository;
    @Mock IdempotencyRepository idempotencyRepository;
    @Mock NotificationPublisher notificationPublisher;
    @InjectMocks EventProcessorService service;

    private EventMessage newEvent() {
        return EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_PLACED")
                .source("order-service")
                .payload(Map.of("orderId", "123"))
                .correlationId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build();
    }

    @Test
    void processEvent_savesAndPublishes_whenNotDuplicate() {
        EventMessage e = newEvent();
        when(idempotencyRepository.existsByEventId(e.getEventId())).thenReturn(false);
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.processEvent(e);

        verify(eventRepository, times(2)).save(any(Event.class));
        verify(idempotencyRepository).save(any(IdempotencyRecord.class));
        verify(notificationPublisher).publish(any());
    }

    @Test
    void processEvent_skips_whenDuplicate() {
        EventMessage e = newEvent();
        when(idempotencyRepository.existsByEventId(e.getEventId())).thenReturn(true);

        service.processEvent(e);

        verify(eventRepository, never()).save(any());
        verify(notificationPublisher, never()).publish(any());
    }
}
