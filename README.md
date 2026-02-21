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


## Testing

### Prerequisites
Make sure the full stack is running:
```cmd
docker compose up --build
```

### Latest Test Results (2026-02-21)

- **Test 1 - Idempotency (Duplicate Delivery):** `PASS`  
  `100` duplicate publishes across `20` threads resulted in exactly `1` row in `events`, `1` in `idempotency_records`, `1` in `notifications`, with event status `PROCESSED`.  
  Evidence: `outputs/Idempotency test.png`

- **Test 2 - Load Test (Throughput & Latency):** `PASS`  
  Locust run (`59s`, `100` users): `8993` total requests, `0` failures, aggregate `153.94` RPS, median `150 ms`, p95 `280 ms`, p99 `1300 ms`.  
  Evidence: `outputs/eventstream_load_report.html`

- **Test 3 - Zero Message Loss (Post Load):** `PASS`  
  `PROCESSING` count is `0`; `processed_events = 4562` and `sent_notifications = 4562` (counts match).  
  Evidence: `outputs/Zero loss test.png`

---

### Test 1 - Idempotency (Duplicate Delivery)

Publishes the same `eventId` 100 times across 20 concurrent threads directly
to RabbitMQ and verifies exactly 1 row was written to the database.


**Copy script into the RabbitMQ container and run:**
```cmd
docker cp test_direct_idempotency.py eventstream-rabbitmq:/tmp/

docker exec -it eventstream-rabbitmq sh -c "apk add --no-cache python3 py3-pip && pip3 install pika psycopg2-binary --break-system-packages -q"

docker exec -it eventstream-rabbitmq sh -c "python3 /tmp/test_direct_idempotency.py"
```

**Expected output:**
```
✓ PASS: Idempotency guaranteed — exactly 1 event written
        despite 100 duplicate deliveries across 20 concurrent threads
```

---

### Test 2 - Load Test (Throughput & Latency)

**Install Locust:**
```cmd
pip install locust
```

**Run for 60 seconds with 100 concurrent users:**
```cmd
locust -f locustfile.py --headless -u 100 -r 10 -t 60s --host http://localhost:8080 --html eventstream_load_report.html
```

Open `eventstream_load_report.html` for the full results including RPS, p50, p95, and p99 latency.

---

### Test 3 - Zero Message Loss (Post Load Test)

After the load test, verify no events were dropped:
```cmd
docker exec -it eventstream-postgres psql -U eventstream -d eventstream -c "SELECT COUNT(*) FROM events WHERE status = 'PROCESSING';"

docker exec -it eventstream-postgres psql -U eventstream -d eventstream -c "SELECT (SELECT COUNT(*) FROM events WHERE status = 'PROCESSED') AS processed_events, (SELECT COUNT(*) FROM notifications WHERE status = 'SENT') AS sent_notifications;"
```

Both counts should match with zero events stuck in `PROCESSING`.


## Test Results

### Test 1 - Idempotency

| Metric | Result |
|---|---|
| Messages published to RabbitMQ | 100 |
| Concurrent threads | 20 |
| Rows in events table | 1 (expected: 1) ✓ |
| Rows in idempotency table | 1 (expected: 1) ✓ |
| Rows in notifications table | 1 (expected: 1) ✓ |
| Event status | PROCESSED ✓ |

**PASS** - Idempotency guaranteed. Exactly 1 event written despite 100 duplicate deliveries across 20 concurrent threads.

---

### Test 2 - Load Test (100 concurrent users, 60s)

| Metric | POST /api/v1/events | Aggregated |
|---|---|---|
| Total Requests | 8,993 | 8,993 |
| Sustained RPS | ~154 | ~154 |
| Failures | 0 (0%) | 0 (0%) |
| p50 latency | 180ms | 150ms |
| p95 latency | 320ms | 280ms |
| p99 latency | 2800ms | 1300ms |
| Max latency | 8100ms | 8100ms |

Open `eventstream_load_report.html` for the full interactive report.

---

### Test 3 - Zero Message Loss (post load test)

| Metric | Count |
|---|---|
| Events with status `PROCESSED` | 4,562 |
| Notifications with status `SENT` | 4,562 |
| Events stuck in `PROCESSING` | 0 |

**PASS** - Zero message loss confirmed. All 4,562 events were fully processed end-to-end with matching notification records.