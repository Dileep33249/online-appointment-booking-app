# 🏥 MediBook Backend — Microservices Architecture

> A production-grade **Doctor Appointment Booking System** built with **Spring Boot 3**, **Spring Cloud**, and **MySQL** — following a distributed microservices architecture with service discovery, API gateway routing, JWT authentication, Stripe payments, Redis caching, and RabbitMQ event-driven notifications.

---

## 📋 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Tech Stack](#-tech-stack)
- [Microservices](#-microservices)
- [API Reference](#-api-reference)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [Service Ports](#-service-ports)
- [Database Design](#-database-design)
- [Security Model](#-security-model)
- [Inter-Service Communication](#-inter-service-communication)
- [Appointment Lifecycle](#-appointment-lifecycle)

---

## 🏗 Architecture Overview

```
┌──────────────┐
│   React UI   │  (Vite + React 18)
│  Port: 5173  │
└──────┬───────┘
       │  HTTP (JWT in Authorization header)
       ▼
┌──────────────────┐
│   API Gateway    │  (Spring Cloud Gateway)
│    Port: 8087    │
└──────┬───────────┘
       │  Routes via Eureka service names
       ▼
┌──────────────────┐
│ Discovery Service│  (Netflix Eureka Server)
│    Port: 8761    │
└──────────────────┘
       │
       ├── auth-service          :8083
       ├── provider-service      :8082
       ├── appointment-service   :8079
       ├── payment-service       :8095
       ├── consultation-service  :8088
       ├── medical-record-service:8089
       ├── notification-service  :8086
       └── admin-service         :8090
```

### Request Flow

1. Frontend sends all requests to the **API Gateway** (`localhost:8087`)
2. Gateway resolves the target service via **Eureka** and forwards the request
3. Each service validates the **JWT token** independently using its own `JwtFilter`
4. Services communicate with each other via **LoadBalanced RestTemplate** through Eureka
5. Asynchronous events (booking, cancellation, payment) flow through **RabbitMQ**

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.2.5 |
| **API Gateway** | Spring Cloud Gateway |
| **Service Discovery** | Netflix Eureka Server |
| **Database** | MySQL 8 (database-per-service) |
| **ORM** | Spring Data JPA / Hibernate |
| **Authentication** | JWT (jjwt 0.11.5) |
| **Payments** | Stripe API (stripe-java 31.3.0) |
| **Caching** | Redis |
| **Messaging** | RabbitMQ |
| **File Uploads** | Cloudinary (signed uploads) |
| **API Docs** | Springdoc OpenAPI / Swagger UI |
| **Build Tool** | Maven |
| **Code Gen** | Lombok |

---

## 📦 Microservices

### 1. Discovery Service
> Netflix Eureka Server — service registry for all microservices

- **Port:** `8761`
- **Dashboard:** `http://localhost:8761`
- All other services register here on startup

### 2. API Gateway Service
> Spring Cloud Gateway — single entry point for the frontend

- **Port:** `8087`
- Routes requests based on URL path predicates
- CORS configured for `localhost:5173` and `localhost:3000`

| Route Pattern | Target Service |
|---|---|
| `/auth/**`, `/users/**` | AUTH-SERVICE |
| `/providers/**` | PROVIDER-SERVICE |
| `/slots/**`, `/appointments/**` | APPOINTMENT-SERVICE |
| `/payments/**` | PAYMENT-SERVICE |
| `/consultations/**` | CONSULTATION-SERVICE |
| `/records/**` | MEDICAL-RECORD-SERVICE |
| `/notifications/**` | NOTIFICATION-SERVICE |
| `/admin/**` | ADMIN-SERVICE |

### 3. Auth Service
> User authentication, JWT token management, profile management, admin bootstrap

- **Port:** `8083`
- **Database:** `medibook_auth`
- Auto-creates a default admin account on startup
- Generates signed Cloudinary upload URLs for profile images
- Supports Google OAuth2 client configuration

### 4. Provider Service
> Doctor/provider profile management, availability, and unavailable dates

- **Port:** `8082`
- **Database:** `medibook_provider`
- Manages doctor specialization, clinic info, consultation fees
- Tracks unavailable dates for schedule blocking
- Redis-cached provider lookups and search results
- Internal endpoints for inter-service provider resolution

### 5. Appointment Service
> Slot management, appointment booking, cancellation, rescheduling, and completion

- **Port:** `8079`
- **Database:** `medibook_appointment`
- **Caching:** Redis for public slot queries
- **Messaging:** RabbitMQ producer for appointment events
- Daily booking cap of **100 patients per doctor per day**
- Smart queue numbering with estimated time windows
- Appointment status lifecycle: `PENDING → CONFIRMED → MET → COMPLETED`

### 6. Payment Service
> Stripe payment integration with PaymentIntent workflow and webhook handling

- **Port:** `8095`
- **Database:** `medibook_payment`
- Creates Stripe PaymentIntents with appointment metadata
- Confirms payments via frontend sync and Stripe webhook
- Automatically notifies `appointment-service` on successful payment
- Supports refunds via Stripe Refund API

### 7. Consultation Service
> Consultation lifecycle management — create, start, complete, prescribe

- **Port:** `8088`
- **Database:** `medibook_consultation`
- Tracks consultation status: `CREATED → IN_PROGRESS → COMPLETED`
- Stores diagnosis, notes, and prescription summaries

### 8. Medical Record Service
> Long-term patient medical records, prescriptions, and attachments

- **Port:** `8089`
- **Database:** `medibook_records`
- Stores structured clinical records per appointment
- Supports prescription items and file attachments

### 9. Notification Service
> In-app notifications powered by RabbitMQ event consumption

- **Port:** `8086`
- **Database:** `medibook_notification`
- RabbitMQ consumer for appointment events (booking, cancellation, reschedule, completion)
- REST endpoints for dispatching reminders and fetching user notifications

### 10. Admin Service
> Platform administration — analytics, provider verification, review moderation

- **Port:** `8090`
- **Database:** `medibook_admin`
- Provider verification task workflow (approve/reject)
- Review reporting and moderation
- Platform-wide analytics overview

---

## 📡 API Reference

### Auth Service (`/auth/**`, `/users/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/signup` | ❌ | Register a new patient or doctor |
| POST | `/auth/login` | ❌ | Login and receive JWT token |
| POST | `/auth/change-password` | ✅ | Change current user password |
| GET | `/users/me` | ✅ | Get current user profile |
| PUT | `/users/me` | ✅ | Update current user profile |
| GET | `/users/upload-signature` | ✅ | Get Cloudinary signed upload params |
| GET | `/users/admin/all` | ✅ ADMIN | List all users |
| GET | `/users/admin/patients` | ✅ ADMIN | List all patients |
| GET | `/users/admin/doctors` | ✅ ADMIN | List all doctors |
| POST | `/users/admin/doctors` | ✅ ADMIN | Create a doctor account |
| PUT | `/users/admin/doctors/{id}` | ✅ ADMIN | Update a doctor account |
| DELETE | `/users/admin/{id}` | ✅ ADMIN | Delete a user |

### Provider Service (`/providers/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/providers` | ❌ | List all providers |
| GET | `/providers/{id}` | ❌ | Get provider by ID |
| GET | `/providers/search?keyword=` | ❌ | Search providers |
| GET | `/providers/specialization?name=` | ❌ | Filter by specialization |
| POST | `/providers/add` | ✅ DOCTOR | Create provider profile |
| GET | `/providers/me` | ✅ DOCTOR | Get own profile |
| PUT | `/providers/me` | ✅ DOCTOR | Update own profile |
| PUT | `/providers/me/availability` | ✅ DOCTOR | Toggle availability |
| GET | `/providers/me/unavailable-dates` | ✅ DOCTOR | Get blocked dates |
| POST | `/providers/me/unavailable-dates` | ✅ DOCTOR | Block a date |
| DELETE | `/providers/me/unavailable-dates` | ✅ DOCTOR | Unblock a date |
| PUT | `/providers/{id}/verify` | ✅ ADMIN | Verify a provider |
| PUT | `/providers/{id}/unverify` | ✅ ADMIN | Unverify a provider |

### Appointment Service — Slots (`/slots/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/slots/public` | ❌ | Public available slots (filterable) |
| GET | `/slots/provider/{providerId}` | ❌ | Slots for a specific provider |
| POST | `/slots` | ✅ DOCTOR | Create a new slot |
| GET | `/slots/my` | ✅ DOCTOR | Get own slots |
| DELETE | `/slots/{id}` | ✅ DOCTOR | Delete a slot |

### Appointment Service — Appointments (`/appointments/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/appointments/book/{slotId}` | ✅ PATIENT | Book a slot |
| GET | `/appointments/my` | ✅ PATIENT | Get my appointments |
| PUT | `/appointments/{id}/cancel` | ✅ PATIENT | Cancel an appointment |
| PUT | `/appointments/{id}/reschedule` | ✅ PATIENT | Reschedule to a new slot |
| GET | `/appointments/provider` | ✅ DOCTOR | Get provider appointments |
| PUT | `/appointments/{id}/meet` | ✅ DOCTOR | Mark patient as met |
| PUT | `/appointments/{id}/complete` | ✅ DOCTOR | Mark appointment complete |
| POST | `/appointments/{id}/internal/confirm-payment` | Internal | Confirm payment (from payment-service) |

### Payment Service (`/payments/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/payments/stripe/config` | ✅ | Get Stripe publishable key |
| POST | `/payments/stripe/create-intent` | ✅ | Create a Stripe PaymentIntent |
| POST | `/payments/stripe/sync` | ✅ | Sync payment after Stripe confirmation |
| POST | `/payments/process` | ✅ | Process a payment |
| GET | `/payments/{paymentId}` | ✅ | Get payment details |
| GET | `/payments/appointment/{id}` | ✅ | Get payment by appointment |
| GET | `/payments/appointment/{id}/status` | ✅ | Check payment status |
| GET | `/payments/my-payments` | ✅ | Get current user's payments |
| GET | `/payments/provider/{providerId}` | ✅ | Get provider's payments |
| POST | `/payments/{paymentId}/refund` | ✅ | Refund a payment |
| POST | `/payments/webhook` | ❌ | Stripe webhook endpoint |

### Consultation Service (`/consultations/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/consultations` | ✅ | Create a consultation |
| GET | `/consultations/{appointmentId}` | ✅ | Get consultation by appointment |
| PUT | `/consultations/{appointmentId}/start` | ✅ | Start a consultation |
| PUT | `/consultations/{appointmentId}/complete` | ✅ | Complete with diagnosis/notes |
| POST | `/consultations/{appointmentId}/prescription` | ✅ | Add prescription summary |

### Medical Record Service (`/records/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/records/{appointmentId}` | ✅ | Create a medical record |
| GET | `/records/patient/{patientId}` | ✅ | Get patient history |
| GET | `/records/{recordId}` | ✅ | Get specific record |
| POST | `/records/{recordId}/attachments` | ✅ | Add attachment |
| POST | `/records/{recordId}/prescriptions` | ✅ | Add prescription item |

### Notification Service (`/notifications/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/notifications/booking-confirmation` | ✅ | Create booking notification |
| POST | `/notifications/cancellation` | ✅ | Create cancellation notification |
| POST | `/notifications/reminder` | ✅ | Schedule a reminder |
| POST | `/notifications/dispatch` | ✅ | Dispatch due reminders |
| GET | `/notifications/user/{userId}` | ✅ | Get user notifications |

### Admin Service (`/admin/**`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/admin/providers/verification-task` | ✅ ADMIN | Create verification task |
| GET | `/admin/providers/pending-verification` | ✅ ADMIN | List pending verifications |
| PUT | `/admin/providers/{taskId}/approve` | ✅ ADMIN | Approve a provider |
| PUT | `/admin/providers/{taskId}/reject` | ✅ ADMIN | Reject a provider |
| POST | `/admin/reviews/report` | ✅ | Report a review |
| GET | `/admin/reviews/reported` | ✅ ADMIN | List reported reviews |
| GET | `/admin/analytics/overview` | ✅ ADMIN | Get platform analytics |

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Maven 3.8+**
- **MySQL 8**
- **Redis** (for caching)
- **RabbitMQ** (for async notifications)
- **Stripe CLI** (for webhook testing)
- **Node.js 18+** (for the frontend)

### 1. Clone and Configure

```bash
git clone <repository-url>
cd online-appointment-app
```

Copy the environment template and fill in your values:

```bash
cp .env.mysql.example .env
```

### 2. Start Infrastructure Services

Start Redis and RabbitMQ (if using Docker):

```bash
docker run -d --name redis -p 6379:6379 redis:alpine
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
```

### 3. Start Backend Services (in order)

```bash
# 1. Discovery Service (must start first)
cd discovery-service/discovery-service
mvn spring-boot:run

# 2. API Gateway
cd api-gateway-service/api-gateway
mvn spring-boot:run

# 3. Business Services (any order)
cd auth-service/auth-service && mvn spring-boot:run
cd provider-service/provider-service && mvn spring-boot:run
cd appointment-service/appointment-service && mvn spring-boot:run
cd payment-service/payment-service && mvn spring-boot:run
cd consultation-service/consultation-service && mvn spring-boot:run
cd medical-record-service/medical-record-service && mvn spring-boot:run
cd notification-service/notification-service && mvn spring-boot:run
cd admin-service/admin-service && mvn spring-boot:run
```

### 4. Start Stripe Webhook Listener

```bash
stripe listen --forward-to localhost:8087/payments/webhook
```

Copy the webhook signing secret and update your `.env`:

```
STRIPE_WEBHOOK_SECRET=whsec_your_secret_here
```

### 5. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`

---

## 🔐 Environment Variables

Create a `.env` file in the root directory (or in each service directory):

Refer to `.env.mysql.example` for the full list of variables. Copy it and fill in your own values:

```bash
cp .env.mysql.example .env
```

> **Note:** JDBC URLs use `createDatabaseIfNotExist=true`, so databases are created automatically on first startup.

---

## 🔌 Service Ports

| Service | Port | Eureka Name |
|---|---|---|
| Discovery Service | `8761` | — |
| API Gateway | `8087` | API-GATEWAY |
| Auth Service | `8083` | AUTH-SERVICE |
| Provider Service | `8082` | PROVIDER-SERVICE |
| Appointment Service | `8079` | APPOINTMENT-SERVICE |
| Payment Service | `8095` | PAYMENT-SERVICE |
| Notification Service | `8086` | NOTIFICATION-SERVICE |
| Consultation Service | `8088` | CONSULTATION-SERVICE |
| Medical Record Service | `8089` | MEDICAL-RECORD-SERVICE |
| Admin Service | `8090` | ADMIN-SERVICE |

---

## 🗄 Database Design

This project follows the **database-per-service** pattern. Each microservice owns its own MySQL schema:

| Service | Database | Key Tables |
|---|---|---|
| auth-service | `medibook_auth` | `users` |
| provider-service | `medibook_provider` | `providers`, `provider_unavailable_dates` |
| appointment-service | `medibook_appointment` | `slots`, `appointments` |
| payment-service | `medibook_payment` | `payments` |
| consultation-service | `medibook_consultation` | `consultations` |
| medical-record-service | `medibook_records` | `medical_records`, `prescription_items`, `record_attachments` |
| notification-service | `medibook_notification` | `notifications` |
| admin-service | `medibook_admin` | `provider_verification_tasks`, `review_reports` |

All tables are auto-generated by Hibernate (`ddl-auto=update`).

---

## 🔒 Security Model

- **JWT-based authentication** — tokens issued by `auth-service` on login
- Token payload contains `userId` and `role`
- Every service has its own `JwtFilter` + `JwtUtil` for independent token validation
- **Role-based access control** with three roles:
  - `PATIENT` — book appointments, make payments, view records
  - `DOCTOR` — manage slots, view appointments, conduct consultations
  - `ADMIN` — verify providers, view analytics, manage users
- Internal service-to-service endpoints use `permitAll()` security rules

---

## 🔗 Inter-Service Communication

| Pattern | Usage |
|---|---|
| **Synchronous (RestTemplate)** | `appointment-service` ↔ `provider-service` for provider lookup |
| **Synchronous (RestTemplate)** | `payment-service` → `appointment-service` for payment confirmation |
| **Asynchronous (RabbitMQ)** | `appointment-service` → `notification-service` for event notifications |

All RestTemplate calls use `@LoadBalanced` annotation with Eureka service names (e.g., `http://provider-service/providers/...`).

### RabbitMQ Configuration

- **Exchange:** `notification-exchange`
- **Queue:** `notification-queue`
- **Routing Key:** `notification-routing-key`
- **Events:** `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`, `APPOINTMENT_RESCHEDULED`, `APPOINTMENT_COMPLETED`, `PAYMENT_SUCCESS`

---

## 🔄 Appointment Lifecycle

```
Patient books slot          Payment succeeds           Doctor marks met
      │                           │                          │
      ▼                           ▼                          ▼
  ┌────────┐    Stripe OK    ┌───────────┐   Doctor    ┌─────────┐   Complete   ┌───────────┐
  │PENDING │ ──────────────► │ CONFIRMED │ ──────────► │   MET   │ ──────────► │ COMPLETED │
  └────────┘                 └───────────┘             └─────────┘             └───────────┘
      │                           │
      │ (no payment)              │ (patient cancels)
      ▼                           ▼
  ┌───────────┐             ┌───────────┐
  │ CANCELLED │             │ CANCELLED │
  └───────────┘             └───────────┘
```

1. **PENDING** — Slot reserved, awaiting payment
2. **CONFIRMED** — Payment successful, appointment officially scheduled
3. **MET** — Doctor has seen the patient
4. **COMPLETED** — Consultation finished with diagnosis and prescription
5. **CANCELLED** — Appointment cancelled by patient (before the scheduled time)

---

## 📄 Swagger API Documentation

Each service exposes Swagger UI when running locally:

| Service | Swagger URL |
|---|---|
| Auth | `http://localhost:8083/swagger-ui.html` |
| Provider | `http://localhost:8082/swagger-ui.html` |
| Appointment | `http://localhost:8079/swagger-ui.html` |
| Payment | `http://localhost:8095/swagger-ui.html` |
| Consultation | `http://localhost:8088/swagger-ui.html` |
| Medical Records | `http://localhost:8089/swagger-ui.html` |
| Notification | `http://localhost:8086/swagger-ui.html` |
| Admin | `http://localhost:8090/swagger-ui.html` |

---

## 📝 License

This project is built for academic and portfolio purposes.

---

**Built with ❤️ using Spring Boot Microservices**
