"""
EventStream Load Test — Locust
================================
Measures end-to-end pipeline throughput:
  POST /events  →  RabbitMQ  →  event-processor  →  notification-service

Prerequisites:
  pip install locust
  docker compose up --build  (EventStream running)

Run (100 users, 10 spawn rate, 60 seconds):
  locust -f locustfile.py --headless -u 100 -r 10 -t 60s \
         --host http://localhost:8080 \
         --html eventstream_load_report.html

Then open eventstream_load_report.html in your browser for the full report.
"""

from locust import HttpUser, task, between
import uuid
import random

EVENT_TYPES = ["ORDER_PLACED", "USER_REGISTERED", "PAYMENT_PROCESSED", "SHIPMENT_DISPATCHED"]
SOURCES = ["order-service", "user-service", "payment-service", "shipping-service"]

class EventProducerUser(HttpUser):
    # Wait 0.1–0.5s between requests per user — adjust for higher RPS
    wait_time = between(0.1, 0.5)

    @task
    def publish_event(self):
        payload = {
            "eventType": random.choice(EVENT_TYPES),
            "source": random.choice(SOURCES),
            "correlationId": str(uuid.uuid4()),
            "payload": {
                "id": str(uuid.uuid4()),
                "amount": round(random.uniform(10.0, 999.99), 2),
                "timestamp": "2025-01-01T00:00:00Z"
            }
        }
        with self.client.post(
            "/api/v1/events",
            json=payload,
            catch_response=True
        ) as response:
            if response.status_code == 202:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code} — {response.text}")

    @task(1)
    def health_check(self):
        """Lightweight health check — lower weight than publish."""
        self.client.get("/api/v1/health")
