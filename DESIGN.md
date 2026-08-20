# User Device Management Service — Design Analysis

## Problem Statement

Design and partially implement an HTTP-based service for managing the devices associated with
users of an online application.

- A user may have multiple devices
- Each device has the following attributes:
  - A unique identifier
  - A device name
  - A device model
- User device list is stored in persistent data store

The following are tasks that the application could support:

1. Add, delete a user to the system
2. List users in the system
3. Add, delete a phone to/from a user
4. List a user's phones
5. Update a user's preferred phone number

The system must support downstream integration for security auditing and analytics. Whenever a
device is added, updated, or removed, the service must publish an event to an external message
broker.

---

## 1. Architecture Overview

**Pattern:** Layered architecture with Spring Boot

```
┌─────────────────────────────────────────────────────┐
│                   REST Clients                        │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP (JSON)
┌──────────────────────▼──────────────────────────────┐
│              Controller Layer                         │
│   UserController    DeviceController                 │
│                     PhoneNumberController             │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│              Service Layer                            │
│   UserService       DeviceService                    │
│                     PhoneNumberService                │
│                        │                             │
│                        └──► EventPublisher            │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│              Repository Layer (Spring Data JPA)       │
│   UserRepository    DeviceRepository                 │
│                     PhoneNumberRepository             │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│              Persistent Data Store                    │
│         (H2 dev / PostgreSQL production)             │
└─────────────────────────────────────────────────────┘

                       ║ (async)
┌══════════════════════╩══════════════════════════════┐
║              Apache Kafka                            ║
║   Topics: device-events, phone-number-events        ║
║   (consumed by security auditing & analytics)       ║
╚═════════════════════════════════════════════════════╝
```

## 2. Domain Model

```
┌───────────────────────┐       1:N       ┌──────────────────┐
│        User           │◄───────────────►│     Device       │
├───────────────────────┤                 ├──────────────────┤
│ id (UUID)             │                 │ id (UUID)        │
│ username              │                 │ deviceName       │
│ email                 │                 │ deviceModel      │
│ preferredPhoneNumberId│                 │ userId (FK)      │
│ version               │                 │ version          │
│ createdAt             │                 │ createdAt        │
│ updatedAt             │                 │ updatedAt        │
└───────────────────────┘                 └──────────────────┘
        │
        │                 1:N             ┌──────────────────┐
        └────────────────────────────────►│   PhoneNumber    │
                                          ├──────────────────┤
                                          │ id (UUID)        │
                                          │ number           │
                                          │ label            │
                                          │ userId (FK)      │
                                          │ version          │
                                          │ createdAt        │
                                          └──────────────────┘
```

### Why Device and PhoneNumber are separate entities

A device (tablet, laptop, smartwatch) doesn't necessarily have a phone number. A phone number
(work, personal) is a contact concept independent of physical hardware. Separating them:

- Avoids nullable phone number fields on devices that don't have one
- Matches the problem statement's separation of "device" tasks and "phone" tasks
- Each entity has a clean, focused schema

### Preferred phone number as a stored pointer

`preferredPhoneNumberId` on User is a nullable FK pointing to one of that user's PhoneNumber records.

## 3. API Design

### Users

| Method | Endpoint                  | Description                    | Status Codes   |
|--------|---------------------------|--------------------------------|----------------|
| POST   | `/api/users`              | Create a user                  | 201, 400, 409  |
| GET    | `/api/users`              | List all users (paginated)     | 200            |
| GET    | `/api/users/{userId}`     | Get a single user              | 200, 404       |
| DELETE | `/api/users/{userId}`     | Delete a user (cascades)       | 204, 404       |

`GET /api/users` supports offset-based pagination via `?page=0&size=20` (Spring Data default).
Returns paginated response with `content`, `totalElements`, `totalPages`, and navigation metadata.
For production at scale, this can be upgraded to cursor-based pagination without changing the
business logic — only the controller and query layer would change.

### Devices

| Method | Endpoint                                       | Description          | Status Codes  |
|--------|------------------------------------------------|----------------------|---------------|
| POST   | `/api/users/{userId}/devices`                  | Add a device         | 201, 400, 404 |
| GET    | `/api/users/{userId}/devices`                  | List user's devices  | 200, 404      |
| GET    | `/api/users/{userId}/devices/{deviceId}`       | Get a single device  | 200, 404      |
| PUT    | `/api/users/{userId}/devices/{deviceId}`       | Update a device      | 200, 400, 404 |
| DELETE | `/api/users/{userId}/devices/{deviceId}`       | Remove a device      | 204, 404      |

### Phone Numbers

| Method | Endpoint                                                | Description              | Status Codes  |
|--------|---------------------------------------------------------|--------------------------|---------------|
| POST   | `/api/users/{userId}/phone-numbers`                     | Add a phone number       | 201, 400, 404, 409 |
| GET    | `/api/users/{userId}/phone-numbers`                     | List user's phone numbers| 200, 404      |
| DELETE | `/api/users/{userId}/phone-numbers/{phoneNumberId}`     | Remove a phone number    | 204, 404      |

### Preferred Phone Number (Singleton Sub-Resource)

| Method | Endpoint                                        | Description                   | Status Codes |
|--------|-------------------------------------------------|-------------------------------|--------------|
| PUT    | `/api/users/{userId}/preferred-phone-number`    | Set preferred phone number    | 200, 400, 404|
| GET    | `/api/users/{userId}/preferred-phone-number`    | Get preferred phone number    | 200, 404     |
| DELETE | `/api/users/{userId}/preferred-phone-number`    | Clear preferred phone number  | 204, 404     |

#### Singleton sub-resource pattern explained

The preferred phone number is exposed as a **singleton sub-resource** rather than a PATCH field on
User. This is a well-established REST pattern for 1:1 relationships that have their own lifecycle.

The URL `/preferred-phone-number` identifies **the relationship** ("this user's preferred one"),
not the phone number itself. The content behind that URL can change (user picks a different number),
but the URL stays the same.

```
/api/users/{userId}/phone-numbers            ← collection (0 to many)
/api/users/{userId}/phone-numbers/123        ← specific item in collection
/api/users/{userId}/preferred-phone-number   ← singleton (0 or 1, pointer into the collection)
```

#### Design Decision: Singleton Sub-Resource vs. PATCH on User

We chose the singleton sub-resource pattern over a PATCH field on User. The singleton provides
clearer semantics (PUT = set, DELETE = clear), independent cacheability, and scales well if the
preference concept grows. The trade-off is slightly more implementation surface (three endpoints
instead of one).

## 4. Request/Response Examples

### Create a user

```http
POST /api/users
Content-Type: application/json

{ "username": "evarhan", "email": "varun.hans@gmail.com" }
```

```json
// 201 Created
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "evarhan",
  "email": "varun.hans@gmail.com",
  "preferredPhoneNumber": null,
  "createdAt": "2026-08-19T23:15:00Z"
}
```

### Add a device

```http
POST /api/users/550e8400-.../devices
Content-Type: application/json

{ "deviceName": "iPad Pro", "deviceModel": "A2377" }
```

```json
// 201 Created
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "deviceName": "iPad Pro",
  "deviceModel": "A2377",
  "createdAt": "2026-08-19T23:15:30Z",
  "updatedAt": "2026-08-19T23:15:30Z"
}
```

### Update a device

```http
PUT /api/users/550e8400-.../devices/660e8400-...
Content-Type: application/json

{ "deviceName": "iPad Pro 12.9", "deviceModel": "A2378" }
```

```json
// 200 OK
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "deviceName": "iPad Pro 12.9",
  "deviceModel": "A2378",
  "createdAt": "2026-08-19T23:15:30Z",
  "updatedAt": "2026-08-19T23:20:00Z"
}
```

### Add a phone number

```http
POST /api/users/550e8400-.../phone-numbers
Content-Type: application/json

{ "number": "+353870933771", "label": "work" }
```

```json
// 201 Created
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "number": "+353870933771",
  "label": "work",
  "createdAt": "2026-08-19T23:16:00Z"
}
```

### Set preferred phone number

```http
PUT /api/users/550e8400-.../preferred-phone-number
Content-Type: application/json

{ "phoneNumberId": "770e8400-e29b-41d4-a716-446655440002" }
```

```json
// 200 OK
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "number": "+353870933771",
  "label": "work"
}
```

### Get preferred phone number

```http
GET /api/users/550e8400-.../preferred-phone-number
```

```json
// 200 OK (if set)
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "number": "+353870933771",
  "label": "work"
}

// 404 Not Found (if not set)
```

### Clear preferred phone number

```http
DELETE /api/users/550e8400-.../preferred-phone-number
// 204 No Content
```

## 5. Event Publishing Design

Events are published to Kafka whenever a device is added, updated, or removed — as explicitly
required by the problem statement. Phone number events are published as a **design enhancement**
beyond stated requirements, to support comprehensive audit trails.

### Topics

- `device-events` — device lifecycle changes **(required by problem statement)**
- `phone-number-events` — phone number lifecycle changes **(design enhancement for auditability)**

### Event schemas

```json
// Device event
{
  "eventId": "uuid",
  "eventType": "DEVICE_ADDED | DEVICE_UPDATED | DEVICE_REMOVED",
  "timestamp": "2026-08-19T23:15:00Z",
  "userId": "uuid",
  "resourceId": "uuid",
  "payload": {
    "deviceName": "iPad Pro",
    "deviceModel": "A2377"
  }
}
```

```json
// Phone number event (design enhancement — not explicitly required)
{
  "eventId": "uuid",
  "eventType": "PHONE_NUMBER_ADDED | PREFERRED_PHONE_NUMBER_CHANGED | PHONE_NUMBER_REMOVED",
  "timestamp": "2026-08-19T23:16:00Z",
  "userId": "uuid",
  "resourceId": "uuid",
  "payload": {
    "number": "+353870933771",
    "label": "work"
  }
}
```

### Delivery guarantees

- Events are published after `saveAndFlush()` / `flush()` confirms the SQL executed successfully,
  ensuring events are only sent for changes that persisted to the database.
- At-least-once delivery semantics — consumers use `eventId` for deduplication.
- Partition key: `userId` — guarantees ordering of events for the same user within a partition.

### What triggers events

| Action                          | Event type               | Topic                 | Required by problem? |
|---------------------------------|--------------------------|-----------------------|---------------------|
| Add a device                    | DEVICE_ADDED             | device-events         | ✅ Yes |
| Update a device                 | DEVICE_UPDATED           | device-events         | ✅ Yes |
| Remove a device                 | DEVICE_REMOVED           | device-events         | ✅ Yes |
| Add a phone number              | PHONE_NUMBER_ADDED       | phone-number-events   | Enhancement |
| Remove a phone number           | PHONE_NUMBER_REMOVED     | phone-number-events   | Enhancement |
| Change preferred phone number   | PREFERRED_PHONE_NUMBER_CHANGED | phone-number-events   | Enhancement |
| Delete user (cascade)           | DEVICE_REMOVED (per device) + PHONE_NUMBER_REMOVED (per phone) | both | Partially (device part) |

## 6. Technology Stack

| Component    | Choice                         | Rationale                                          |
|--------------|--------------------------------|----------------------------------------------------|
| Language     | Java 17                        | LTS, modern features (records, sealed classes)     |
| Framework    | Spring Boot 3.5                | Industry standard, batteries included              |
| Persistence  | Spring Data JPA + Hibernate    | Reduces boilerplate, flexible query derivation     |
| Dev DB       | H2 in-memory                   | Zero-config for local dev/testing                  |
| Prod DB      | PostgreSQL                     | Robust, well-supported relational DB               |
| Messaging    | Apache Kafka                   | High-throughput, durable, supports multiple consumers |
| Build        | Maven                          | Stable dependency management                       |
| Testing      | JUnit 5 + Mockito + EmbeddedKafka | Standard Spring testing stack                  |
| Validation   | Jakarta Bean Validation        | Declarative input validation                       |
| Observability| Spring Boot Actuator           | Health checks, metrics, and info endpoints out of the box — zero custom code |
| API Docs     | SpringDoc OpenAPI (Swagger UI) | Auto-generates interactive API documentation from controllers — browse at `/swagger-ui/index.html` |
| DB Migrations| Flyway                         | Versioned, repeatable schema migrations — same schema in every environment |
| DB Indexing  | FK indexes in Flyway migrations | Indexes on `userId` FKs — included in migration scripts for efficient nested resource queries |

## 7. Package Structure

```
com.example.devicemanagement
├── UserDeviceManagementApplication.java
├── controller/
│   ├── UserController.java
│   ├── DeviceController.java
│   └── PhoneNumberController.java
├── dto/
│   ├── CreateUserRequest.java
│   ├── CreateDeviceRequest.java
│   ├── UpdateDeviceRequest.java
│   ├── CreatePhoneNumberRequest.java
│   ├── SetPreferredPhoneNumberRequest.java
│   ├── UserResponse.java
│   ├── DeviceResponse.java
│   └── PhoneNumberResponse.java
├── mapper/
│   ├── UserMapper.java
│   ├── DeviceMapper.java
│   └── PhoneNumberMapper.java
├── model/
│   ├── User.java
│   ├── Device.java
│   └── PhoneNumber.java
├── repository/
│   ├── UserRepository.java
│   ├── DeviceRepository.java
│   └── PhoneNumberRepository.java
├── service/
│   ├── UserService.java
│   ├── DeviceService.java
│   └── PhoneNumberService.java
├── event/
│   ├── DomainEvent.java              (sealed interface)
│   ├── DeviceEvent.java              (implements DomainEvent)
│   ├── PhoneNumberEvent.java         (implements DomainEvent)
│   ├── DevicePayload.java            (typed event payload)
│   ├── PhoneNumberPayload.java       (typed event payload)
│   ├── EventPublisher.java           (interface)
│   ├── LoggingEventPublisher.java    (@Profile("dev") — logs events)
│   └── KafkaEventPublisher.java      (@Profile("prod") — sends to Kafka)
└── exception/
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    └── GlobalExceptionHandler.java
```

### Mapper Layer Design Decision

The `mapper/` package contains manual mapper classes that convert between JPA entities and DTOs.

**Why a dedicated mapper layer:**
- The entity shape differs from the API response shape (e.g. `User.preferredPhoneNumberId` is a
  UUID FK in the database, but `UserResponse.preferredPhoneNumber` is a full embedded object in
  the API response). Something must resolve that FK into an object — that's the mapper's job.
- Keeps the service layer focused on business logic, not response formatting.
- Keeps entities free of API concerns (no `toResponse()` methods on entities).
- Allows multiple response shapes per entity in the future (e.g. summary vs. detailed).

**Pattern chosen: Manual mapper classes** — full control, can inject dependencies, explicit and
debuggable. For production with more entities, MapStruct would be the recommended upgrade.

**Data flow:**

```
Controller receives request DTO
    │
    ▼
Service performs business logic (uses entities + repositories)
    │
    ▼
Service calls Mapper to convert entity → response DTO
    │  (mapper resolves FKs, formats dates, assembles nested objects)
    ▼
Service returns DTO to Controller
    │
    ▼
Controller returns DTO as HTTP response
```

`EventPublisher` is an interface with two implementations selected by Spring profile:
- **`LoggingEventPublisher`** (`@Profile("dev")`) — logs events to console, no Kafka needed.
- **`KafkaEventPublisher`** (`@Profile("prod")`) — publishes to Kafka topics with `userId` as
  partition key. Uses a single private `publish()` method via the `DomainEvent` sealed interface.

The dev profile is the default — the app starts without Kafka. For production, activate the
`prod` profile: `mvn spring-boot:run -Dspring-boot.run.profiles=prod`.

## 8. Error Response Format

All error responses follow [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807):

```json
// 404 Not Found
{
  "title": "Resource Not Found",
  "status": 404,
  "detail": "User with id '550e8400-...' not found"
}
```

```json
// 400 Bad Request (validation)
{
  "title": "Validation Error",
  "status": 400,
  "detail": "Request body contains invalid fields",
  "violations": [
    { "field": "deviceName", "message": "must not be blank" },
    { "field": "deviceModel", "message": "must not be blank" }
  ]
}
```

```json
// 409 Conflict
{
  "title": "Duplicate Resource",
  "status": 409,
  "detail": "User with username 'evarhan' already exists"
}
```

## 9. Edge Cases & Validation

| Scenario                                          | Behaviour                                              |
|---------------------------------------------------|--------------------------------------------------------|
| Delete user                                       | Cascades: removes all devices + phone numbers, publishes events for each |
| Delete a phone number that is the preferred one   | Clears `preferredPhoneNumberId` on User automatically  |
| Set preferred to a phone number of another user   | 400 Bad Request                                        |
| Set preferred to a non-existent phone number      | 404 Not Found                                          |
| Duplicate username                                | 409 Conflict                                           |
| Invalid phone number format                       | 400 Bad Request (E.164 validation)                     |
| Duplicate phone number for same user              | 409 Conflict                                           |
| GET preferred when none is set                    | 404 Not Found                                          |
| Update a device that doesn't exist                | 404 Not Found                                          |
| Update a device belonging to another user         | 404 Not Found (don't leak existence)                   |
| Device name blank or exceeds 100 chars            | 400 Bad Request                                        |
| Device model blank or exceeds 50 chars            | 400 Bad Request                                        |
| Concurrent updates to same device                 | 409 Conflict (`@Version` optimistic locking — JPA throws `OptimisticLockException`) |
