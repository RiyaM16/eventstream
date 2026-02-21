package com.eventstream.service;

import com.eventstream.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationSender {
    /**
     * Stub — plug in real delivery here: email (SES/SMTP), Slack webhook, push, etc.
     */
    public void send(Notification n) {
        log.info("[NOTIFICATION SENT] id={} eventId={} type={} correlationId={}",
                n.getNotificationId(), n.getEventId(), n.getEventType(), n.getCorrelationId());
    }
}
