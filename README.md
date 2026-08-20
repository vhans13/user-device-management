# User Device Management Service

An HTTP-based service for managing devices and phone numbers associated with users of an online application. Publishes events to Apache Kafka whenever devices are added, updated, or removed.

## Tech Stack

- Java 17 + Spring Boot 3.5
- Spring Data JPA + H2 (in-memory for dev; swap to PostgreSQL for production via config)
- Apache Kafka (event publishing)
- Flyway (database migrations)
- Spring Boot Actuator (health/metrics)
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito + EmbeddedKafka (testing)

## How to Run

### Prerequisites

- Java 17+
- Maven 3.8+
- Apache Kafka (only needed for prod profile — optional for development)

### Quick start (dev profile — default, no Kafka needed)

The app starts with the `dev` profile by default. Events are logged to console instead of
being sent to Kafka. No external infrastructure required.

```bash
cd user-device-management
mvn spring-boot:run
```

### With Kafka (prod profile, via Docker)

```bash
# Start Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk \
  apache/kafka:latest

# Create required topics
docker exec kafka /opt/kafka/bin/kafka-topics.sh --create --topic device-events --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092
docker exec kafka /opt/kafka/bin/kafka-topics.sh --create --topic phone-number-events --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092

# Run the application with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Available endpoints

Once running, access:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html` (interactive API docs — try all endpoints here)
- **Health check:** `http://localhost:8080/actuator/health`
- **Metrics:** `http://localhost:8080/actuator/metrics`
- **H2 Console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:devicedb`)

## How to Test

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=DeviceServiceTest
mvn test -Dtest=UserControllerTest
mvn test -Dtest=EventPublisherTest
```

### Test coverage

| Test class | Type | What it verifies |
|-----------|------|-----------------|
| `DeviceServiceTest` | Unit (Mockito) | CRUD operations, event publishing on add/update/delete |
| `PhoneNumberServiceTest` | Unit (Mockito) | Add, delete, set preferred, duplicate detection |
| `UserServiceTest` | Unit (Mockito) | Create, get, list, delete cascade, batch fetch |
| `UserControllerTest` | Slice (MockMvc) | HTTP status codes, validation, pagination |
| `DeviceControllerTest` | Slice (MockMvc) | Device endpoints, validation, status codes |
| `PhoneNumberControllerTest` | Slice (MockMvc) | Phone number endpoints, preferred phone, validation |
| `DeviceMapperTest` | Unit | Field mapping correctness |
| `PhoneNumberMapperTest` | Unit | Field mapping correctness |
| `UserMapperTest` | Unit | Field mapping with/without preferred phone |
| `EventPublisherTest` | Integration (EmbeddedKafka) | Events reach Kafka with correct key and payload |

## API Quick Reference

### Users
```
POST   /api/users                              Create a user
GET    /api/users?page=0&size=20               List users (paginated)
GET    /api/users/{userId}                     Get a user
DELETE /api/users/{userId}                     Delete a user (cascades)
```

### Devices
```
POST   /api/users/{userId}/devices             Add a device
GET    /api/users/{userId}/devices             List devices
GET    /api/users/{userId}/devices/{deviceId}  Get a device
PUT    /api/users/{userId}/devices/{deviceId}  Update a device
DELETE /api/users/{userId}/devices/{deviceId}  Remove a device
```

### Phone Numbers
```
POST   /api/users/{userId}/phone-numbers                 Add a phone number
GET    /api/users/{userId}/phone-numbers                 List phone numbers
DELETE /api/users/{userId}/phone-numbers/{phoneNumberId}  Remove a phone number
```

### Preferred Phone Number (singleton sub-resource)
```
PUT    /api/users/{userId}/preferred-phone-number   Set preferred
GET    /api/users/{userId}/preferred-phone-number   Get preferred
DELETE /api/users/{userId}/preferred-phone-number   Clear preferred
```

## Assumptions

1. **Device and Phone Number are separate concepts** — a device (tablet, laptop, watch) doesn't necessarily have a phone number. They have independent lifecycles and are managed through separate endpoints.
2. **Phone number format** — validated as E.164 (e.g. `+353870933771`)
3. **No security** — out of scope. Production would add OAuth2/JWT on the API (Spring Security) and SASL_SSL on Kafka (encrypted, authenticated connections)
4. **Cascade delete** — deleting a user removes all their devices/phone numbers and publishes events
5. **Device uniqueness** — by UUID only; duplicate names/models allowed
6. **Phone number uniqueness** — a user cannot add the same number twice (409); different users may share a number
7. **Phone number events** — design enhancement beyond stated requirements (problem only mandates device events)

## What I'd Address with More Time

- **Authentication & RBAC** — OAuth2/JWT via Spring Security
- **Observability** — structured logging, distributed tracing, Prometheus/Grafana
- **Cursor-based pagination** — upgrade from offset-based when scale requires it
- **API versioning** — `/api/v1/` prefix when consumers depend on the contract
- **Containerization** — Dockerfile + docker-compose for full local stack
- **Rate limiting** — protect endpoints from abuse
- **Idempotency keys** — for safe POST retries

## Design Document

See [DESIGN.md](DESIGN.md) for the full design analysis including architecture diagrams, API design decisions, event publishing strategy, and technology justifications.
