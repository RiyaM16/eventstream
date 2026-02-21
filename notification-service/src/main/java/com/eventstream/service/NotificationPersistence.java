package com.eventstream.service;

import com.eventstream.dto.NotificationMessage;
import com.eventstream.entity.Notification;
import com.eventstream.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Separate bean so @Transactional goes through the Spring proxy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPersistence {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification savePending(NotificationMessage msg) {
        log.info("Saving notification [{}] for event [{}]",
                msg.getNotificationId(), msg.getEventId());

        Notification notification = Notification.builder()
                .notificationId(msg.getNotificationId())
                .eventId(msg.getEventId())
                .eventType(msg.getEventType())
                .correlationId(msg.getCorrelationId())
                .metadata(msg.getMetadata())
                .status(Notification.NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        return notificationRepository.save(notification);
        // Transaction commits here — PENDING record durably written
    }

    @Transactional
    public void markSent(Notification notification) {
        notification.setStatus(Notification.NotificationStatus.SENT);
        notification.setSentAt(Instant.now());
        notificationRepository.save(notification);
        log.info("Notification [{}] marked SENT", notification.getNotificationId());
        // Transaction commits here — SENT record durably written
    }
}
