package com.eventstream.listener;

import com.eventstream.config.RabbitConfig;
import com.eventstream.dto.EventMessage;
import com.eventstream.service.EventProcessorService;
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
public class EventQueueListener {

    private final EventProcessorService processorService;

    @RabbitListener(queues = RabbitConfig.EVENT_PROCESSOR_QUEUE)
    public void onMessage(EventMessage event,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.debug("Received event [{}] from queue", event.getEventId());
        try {
            processorService.processEvent(event);
            channel.basicAck(deliveryTag, false);  // ACK — remove from queue
        } catch (Exception e) {
            log.error("Failed processing event [{}]: {}", event.getEventId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);  // NACK → DLQ
        }
    }
}
