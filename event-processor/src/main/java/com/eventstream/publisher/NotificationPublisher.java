package com.eventstream.publisher;

import com.eventstream.config.RabbitConfig;
import com.eventstream.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(NotificationMessage notification) {
        String routingKey = "notification." + notification.getEventType().toLowerCase().replace("_", ".");
        log.info("Publishing notification [{}] for event [{}]",
                notification.getNotificationId(), notification.getEventId());
        rabbitTemplate.convertAndSend(RabbitConfig.NOTIFICATIONS_EXCHANGE, routingKey, notification);
    }
}
