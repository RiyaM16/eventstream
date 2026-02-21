package com.eventstream.listener;

import com.eventstream.config.RabbitConfig;
import com.eventstream.dto.NotificationMessage;
import com.eventstream.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationQueueListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void onMessage(NotificationMessage notification,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.debug("Received notification [{}]", notification.getNotificationId());
        try {
            notificationService.processNotification(notification);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed processing notification [{}]: {}",
                    notification.getNotificationId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false); // → DLQ
        }
    }
}
