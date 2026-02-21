"""
Direct Idempotency Test — publishes the SAME eventId to RabbitMQ 100 times
===========================================================================
This is the true idempotency test: bypasses the producer (which always generates
a new UUID) and publishes directly to RabbitMQ with a fixed eventId.

The processor should write exactly 1 row to the events table and 1 row to
the idempotency table, regardless of how many times the same message arrives.

Prerequisites:
  pip install pika psycopg2-binary
  docker compose up --build  (EventStream running)

Run:
  python test_direct_idempotency.py

"""

import pika
import psycopg2
import json
import uuid
import time
import concurrent.futures
from datetime import datetime, timezone

RABBITMQ_CONFIG = {
    "host": "127.0.0.1",
    "port": 5672,
    "credentials": pika.PlainCredentials("guest", "guest"),
}
EXCHANGE = "events.exchange"
ROUTING_KEY = "event.order.placed"

DB_CONFIG = {
    "host": "eventstream-postgres",
    "port": 5432,
    "dbname": "eventstream",
    "user": "eventstream",
    "password": "eventstream",
}

DUPLICATE_COUNT = 100
FIXED_EVENT_ID = str(uuid.uuid4())
FIXED_CORRELATION_ID = str(uuid.uuid4())

def publish_message(_):
    """Publish the same eventId to RabbitMQ."""
    connection = pika.BlockingConnection(pika.ConnectionParameters(**RABBITMQ_CONFIG))
    channel = connection.channel()

    message = {
        "eventId": FIXED_EVENT_ID,          # same every time — this is the key
        "eventType": "ORDER_PLACED",
        "source": "idempotency-test",
        "correlationId": FIXED_CORRELATION_ID,
        "payload": {"orderId": "ord-idem-001", "amount": 99.99},
        "timestamp": datetime.now(timezone.utc).isoformat()
    }

    channel.basic_publish(
        exchange=EXCHANGE,
        routing_key=ROUTING_KEY,
        body=json.dumps(message),
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2  # persistent
        )
    )
    connection.close()
    return True

def run_direct_idempotency_test():
    print("=" * 60)
    print("EventStream — Direct Idempotency Test")
    print("=" * 60)
    print(f"Fixed eventId:       {FIXED_EVENT_ID}")
    print(f"Fixed correlationId: {FIXED_CORRELATION_ID}")
    print(f"Duplicate count:     {DUPLICATE_COUNT}")
    print(f"Threads:             20")
    print()

    # Publish 100 copies of the same message concurrently
    print(f"Publishing {DUPLICATE_COUNT} identical messages to RabbitMQ...")
    with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
        results = list(executor.map(publish_message, range(DUPLICATE_COUNT)))
    published = sum(1 for r in results if r)
    print(f"Successfully published: {published}/{DUPLICATE_COUNT}")

    # Wait for processor to consume all messages
    print("\nWaiting 5 seconds for processor to consume all messages...")
    time.sleep(5)

    # Query DB
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()

    cur.execute("SELECT COUNT(*) FROM events WHERE event_id = %s", (FIXED_EVENT_ID,))
    event_rows = cur.fetchone()[0]

    cur.execute("SELECT COUNT(*) FROM idempotency WHERE event_id = %s", (FIXED_EVENT_ID,))
    idempotency_rows = cur.fetchone()[0]

    cur.execute("SELECT COUNT(*) FROM notifications WHERE correlation_id = %s", (FIXED_CORRELATION_ID,))
    notification_rows = cur.fetchone()[0]

    cur.execute("SELECT status FROM events WHERE event_id = %s", (FIXED_EVENT_ID,))
    row = cur.fetchone()
    status = row[0] if row else "NOT FOUND"

    cur.close()
    conn.close()

    print("\n" + "=" * 60)
    print("RESULTS")
    print("=" * 60)
    print(f"Messages published to RabbitMQ:    {DUPLICATE_COUNT}")
    print(f"Rows in events table:              {event_rows}  (expected: 1)")
    print(f"Rows in idempotency table:         {idempotency_rows}  (expected: 1)")
    print(f"Rows in notifications table:       {notification_rows}  (expected: 1)")
    print(f"Event status:                      {status}  (expected: PROCESSED)")
    print()

    if event_rows == 1 and idempotency_rows == 1:
        print("✓ PASS: Idempotency guaranteed — exactly 1 event written")
        print(f"        despite {DUPLICATE_COUNT} duplicate deliveries across 20 concurrent threads")
        print("──────────────────────────────────────────────────────────")
    else:
        print(f"✗ FAIL: Expected 1 event row, got {event_rows}")
        print("Check processor logs: docker compose logs event-processor")

if __name__ == "__main__":
    run_direct_idempotency_test()
