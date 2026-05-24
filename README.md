🚀 Idempotency Gateway (Pay-Once Protocol)

A Spring Boot payment gateway service that ensures exactly-once payment processing using an Idempotency-Key mechanism.

It prevents:

duplicate payments caused by retries
network timeout double-charging
race conditions under concurrent requests
payload tampering using same key

This project simulates a real-world fintech payment gateway design (Stripe/PayPal-style idempotency system).

📌 Business Problem

Clients of a payment processor experience network instability.

When a payment request times out:

the client retries automatically
the server processes both requests
❌ the customer is charged twice
✅ Solution

This system introduces an Idempotency Layer that ensures:

Same request → processed only once
Duplicate retry → returns cached response
Same key + different payload → rejected
Concurrent requests → safely synchronized
⚙️ Tech Stack
Java 17
Spring Boot 3.x
Spring Web
Spring Validation
Spring Security (filter chain present)
Maven
In-memory repository (Map-based)
🧠 Core System Capabilities

✔ Exactly-once payment processing
✔ Idempotent retry handling
✔ Payload integrity validation
✔ Race condition protection
✔ In-flight request synchronization
✔ Cached response replay

📂 Project Structure
com.igirepay.gateway
│
├── config          → Configuration classes
├── controller      → REST API endpoints
├── exception       → Custom exceptions
├── model           → DTOs (PaymentRequest, PaymentResponse)
├── repository      → In-memory idempotency storage
├── security        → Security filters (JWT chain)
├── services        → Business logic (IdempotencyService)
└── util            → Hashing utilities
🚀 Setup Instructions
1. Prerequisites
Java 17+
Maven 3+
Git
2. Clone Repository
git clone https://github.com/your-username/idempotency-gateway.git
cd idempotency-gateway
3. Build Project
mvn clean install
4. Run Application
mvn spring-boot:run
5. Application Access
API Base URL:
http://localhost:8080
Swagger UI:
http://localhost:8080/swagger-ui/index.html
📡 API Documentation
🔹 Process Payment
Endpoint
POST /api/process-payment
Headers
Header	Required	Description
Idempotency-Key	YES	Unique request identifier
Request Body
{
  "amount": 100,
  "currency": "RWF"
}
Successful Response
{
  "transactionId": "TX-1700000000000",
  "status": "SUCCESS",
  "message": "Charged 100 RWF",
  "amount": 100,
  "currency": "RWF",
  "timestamp": "2026

  <img width="1555" height="874" alt="Screenshot 2026-05-23 073439" src="https://github.com/user-attachments/assets/765accb0-db6a-4f9e-8abe-d9587673f42b" />
