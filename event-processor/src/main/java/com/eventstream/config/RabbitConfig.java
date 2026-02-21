package com.eventstream.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // ── Exchange names ────────────────────────────────────────────────────────
    public static final String EVENTS_EXCHANGE        = "events.exchange";
    public static final String NOTIFICATIONS_EXCHANGE = "notifications.exchange";

    // ── Queue names ───────────────────────────────────────────────────────────
    public static final String EVENT_PROCESSOR_QUEUE = "event.processor.queue";
    public static final String NOTIFICATION_QUEUE    = "notification.queue";
    public static final String EVENT_DLQ             = "event.processor.dlq";
    public static final String NOTIFICATION_DLQ      = "notification.dlq";

    // ── Exchanges ─────────────────────────────────────────────────────────────
    @Bean public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }
    @Bean public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }

    // ── Dead letter queues ────────────────────────────────────────────────────
    @Bean public Queue eventDlq() {
        return QueueBuilder.durable(EVENT_DLQ).build();
    }
    @Bean public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    // ── Main queues (point to DLQ on failure) ─────────────────────────────────
    @Bean public Queue eventProcessorQueue() {
        return QueueBuilder.durable(EVENT_PROCESSOR_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", EVENT_DLQ)
                .build();
    }
    @Bean public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────
    @Bean public Binding eventProcessorBinding() {
        return BindingBuilder.bind(eventProcessorQueue()).to(eventsExchange()).with("event.#");
    }
    @Bean public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationsExchange()).with("notification.#");
    }

    // ── Jackson converter with Java 8 time support ────────────────────────────
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }

    // Manual ack factory — used by @RabbitListener consumers
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory cf) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(messageConverter());
        f.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return f;
    }
}
