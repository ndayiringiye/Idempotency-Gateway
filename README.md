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
Mysql
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

  ## 7. Distributed-Ready Idempotency Design (Developer's Choice)

In a real-world production deployment at IgirePay Technologies, a single server instance running an in-memory storage component (such as a local `ConcurrentHashMap`) introduces two critical points of failure:
1. **Data Loss on Restart:** If the application crashes or undergoes a routine deployment, all active and cached idempotency keys are completely wiped out, exposing clients to immediate double-charging risks on retries.
2. **Split-Brain & Synchronization Failures:** Modern cloud environments scale horizontally behind a Load Balancer. If Request A hits **Server Instance 1** and a duplicate/retry Request B hits **Server Instance 2**, the local memory spaces cannot communicate. This completely breaks both the *Duplicate Request* validation and the *In-Flight Lock* protections.

To bridge standard computer science fundamentals with modern backend scaling, this system replaces localized variables with a **Distributed-Ready Centralized Lock and State Engine Pattern**.

### Architectural Blueprint
Instead of holding local objects, the `IdempotencyGateway` delegates atomicity and persistence to a high-throughput, centralized key-value engine (e.g., Redis or an equivalent standalone data layer) using specific atomicity features:

* **Atomic Distributed Locking:** Instead of standard Java thread blocking, the application leverages atomic commands (such as Redis `SETNX` — *Set if Not Exists*) to claim an execution lock across all server instances simultaneously.
* **Auto-Expiring State (TTL):** To keep storage lean and performance optimal, the engine uses an explicit Time-To-Live (TTL) configuration. Idempotency records automatically expire after **24 hours**, safely clearing out old transactions while preserving data integrity during critical network windows.

### Technical State Transitions

| Phase | Action / Command | Expected Behavior across Cluster |
| :--- | :--- | :--- |
| **Lock Acquisition** | `SET key value NX PX 10000` | Instantly locks the key cluster-wide for 10 seconds. Returns failure if an in-flight request is already active on any other node. |
| **State Update** | `SET key success_payload` | Transitions the state from `IN_PROGRESS` to `SUCCESS` with an explicit 24-hour expiration threshold. |
| **Failure Recovery** | Automatic TTL Expiry | If a payment worker node crashes midway during a 3rd party API call, the lock naturally expires, preventing a permanent deadlock. |

### Why This Matters for Fintech
By moving the validation logic to an independent state manager, IgirePay Technologies can confidently spin up hundreds of containerized API instances to handle peak traffic demands without ever worrying about race conditions, split-brain routing errors, or accidental double-billing.
