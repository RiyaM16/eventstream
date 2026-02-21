package com.eventstream.service;

import com.eventstream.dto.NotificationMessage;
import com.eventstream.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    // Injected beans — all @Transactional calls go through proxies
    private final NotificationPersistence notificationPersistence;
    private final NotificationSender notificationSender;

    public void processNotification(NotificationMessage msg) {
        // Step 1 — save PENDING; transaction commits before this returns
        Notification notification = notificationPersistence.savePending(msg);

        // Step 2 — send AFTER PENDING is committed
        notificationSender.send(notification);

        // Step 3 — mark SENT in its own transaction
        notificationPersistence.markSent(notification);
    }
}
