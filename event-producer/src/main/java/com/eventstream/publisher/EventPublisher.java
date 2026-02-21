package com.eventstream.publisher;

import com.eventstream.config.RabbitConfig;
import com.eventstream.dto.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(EventMessage event) {
        String routingKey = "event." + event.getEventType().toLowerCase().replace("_", ".");
        log.info("Publishing event [{}] type [{}] routing-key [{}]",
                event.getEventId(), event.getEventType(), routingKey);
        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, routingKey, event);
        log.info("Event [{}] published OK", event.getEventId());
    }
}
