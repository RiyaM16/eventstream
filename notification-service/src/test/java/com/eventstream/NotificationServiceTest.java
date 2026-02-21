package com.eventstream;

import com.eventstream.dto.NotificationMessage;
import com.eventstream.entity.Notification;
import com.eventstream.repository.NotificationRepository;
import com.eventstream.service.NotificationSender;
import com.eventstream.service.NotificationService;
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
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationSender notificationSender;
    @InjectMocks NotificationService service;

    @Test
    void processNotification_savesAndSends() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationMessage msg = NotificationMessage.builder()
                .notificationId(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_PLACED")
                .correlationId(UUID.randomUUID().toString())
                .metadata(Map.of("source", "test"))
                .createdAt(Instant.now())
                .build();

        service.processNotification(msg);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationSender).send(any(Notification.class));
    }
}
