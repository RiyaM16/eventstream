# EventStream

Event-driven microservices - Java 17 + Spring Boot 3 + RabbitMQ + PostgreSQL.

**One command to run everything. Just Docker.**

## Quickstart

```bash
docker compose up --build
```

First run compiles all three services inside Docker (~5 min). Subsequent runs are fast.

### Send an event

```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "ORDER_PLACED",
    "source": "order-service",
    "payload": { "orderId": "ord-123", "amount": 99.99 }
  }'
```

```agsl
curl -X POST http://localhost:8080/api/v1/events -H "Content-Type: application/json" -d "{\"eventType\":\"ORDER_PLACED\",\"source\":\"order-service\",\"payload\":{\"orderId\":\"123\"}}"
```

### Watch the pipeline

```bash
docker compose logs -f
```

You'll see the event flow through all three services in the logs.

### RabbitMQ UI

Open http://localhost:15672 - login: `guest` / `guest`

View exchanges, queues, message rates, and the DLQ.

### Inspect the database

```bash
docker exec -it eventstream-postgres psql -U eventstream -d eventstream
SELECT event_id, event_type, status, processed_at FROM events;
SELECT notification_id, event_id, status, sent_at FROM notifications;
\q
```

## Architecture

```
POST /events
     |
  event-producer :8080
     | publishes to events.exchange
     |
  RabbitMQ
  events.exchange --> event.processor.queue --> event.processor.dlq (on failure)
     |
  event-processor :8081
     | writes to PostgreSQL (events + idempotency tables)
     | publishes to notifications.exchange
     |
  RabbitMQ
  notifications.exchange --> notification.queue --> notification.dlq (on failure)
     |
  notification-service :8082
     | writes to PostgreSQL (notifications table)
     | calls NotificationSender (stub — plug in email/webhook/etc)
```

## Services

| Service | Port | Role |
|---|---|---|
| event-producer | 8080 | REST API, validates and publishes events |
| event-processor | 8081 | Consumes queue, idempotency check, persists, triggers notifications |
| notification-service | 8082 | Consumes queue, persists, sends notifications |
| rabbitmq | 5672 / 15672 | Message broker with management UI |
| postgres | 5432 | PostgreSQL database |

## Reset

```bash
docker compose down -v   # wipes all data
docker compose up --build
```
