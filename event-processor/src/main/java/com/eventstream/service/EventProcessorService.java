package com.eventstream.service;

import com.eventstream.dto.EventMessage;
import com.eventstream.dto.NotificationMessage;
import com.eventstream.publisher.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessorService {

    // Injected bean — calls to eventPersistence go through the Spring proxy,
    // so @Transactional on persistEvent() is properly honoured
    private final EventPersistence eventPersistence;
    private final NotificationPublisher notificationPublisher;

    public void processEvent(EventMessage msg) {
        // Step 1 — DB work runs in its own transaction via the proxy.
        //           Transaction commits when persistEvent() returns.
        NotificationMessage notification = eventPersistence.persistEvent(msg);

        // Step 2 — publish only AFTER the commit.
        //           DB writes are safe even if publish throws.
        if (notification != null) {
            notificationPublisher.publish(notification);
        }
    }
}
