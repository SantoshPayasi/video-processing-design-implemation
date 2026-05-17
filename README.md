# Netflix — Microservices System Design

A microservices-based backend system modelled after Netflix's core video streaming pipeline. Built with Spring Boot, Apache Kafka, AWS S3, Redis, and MySQL.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Client / Browser                           │
└───────────┬──────────────────┬──────────────────┬──────────────────┘
            │                  │                  │
            ▼                  ▼                  ▼
  ┌─────────────────┐  ┌──────────────┐  ┌──────────────────┐
  │  content-service│  │ video-service│  │streaming-service │
  │   (Port 8080)   │  │  (Port 8083) │  │   (Port 8082)    │
  │                 │  │              │  │                  │
  │  Movie metadata │  │ Upload video │  │ Serve play URLs  │
  │  MySQL storage  │  │ to S3        │  │ Redis cache      │
  └─────────────────┘  └──────┬───────┘  └──────────────────┘
                              │                  ▲
                    Kafka: video.uploaded         │
                              │         Kafka: video.encoding.complete
                              ▼                  │
                    ┌──────────────────┐         │
                    │ encoding-service │──────────┘
                    │   (Port 8081)    │
                    │                  │
                    │ Transcode video  │
                    │ HLS to S3        │
                    └──────────────────┘
```

### Video Processing Flow

1. Client uploads a video file to **video-service**
2. video-service stores the raw file in S3 and publishes a `video.uploaded` Kafka event
3. **encoding-service** consumes the event, transcodes the video into HLS segments, and uploads them to S3, then publishes `video.encoding.complete`
4. **streaming-service** consumes the completion event, caches the HLS master playlist URL in Redis, and makes it available for playback
5. Clients request playback from streaming-service and receive a cached HLS URL directly

---

## Services

| Service | Port | Responsibility | Data Store |
|---|---|---|---|
| `content-service` | 8080 | Movie metadata CRUD | MySQL |
| `encoding-service` | 8081 | Video transcoding (HLS) | AWS S3 |
| `streaming-service` | 8082 | Serve & cache playback URLs | Redis + AWS S3 |
| `video-service` | 8083 | Raw video upload | AWS S3 |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.6 (Java 17) |
| Messaging | Apache Kafka |
| Metadata DB | MySQL 8 |
| Cache | Redis |
| Object Storage | AWS S3 (S3-compatible endpoint) |
| ORM | Spring Data JPA / Hibernate |
| Containerization | Docker Compose |

---

## Infrastructure

| Component | Port | Purpose |
|---|---|---|
| MySQL | 3306 | Content metadata storage (`content_db`) |
| Redis | 6379 | Streaming URL cache |
| Kafka | 9092 | Async inter-service messaging |
| Zookeeper | 2181 | Kafka cluster coordination |

---

## Project Structure

```
Netflix/
├── content-service/                  # Movie metadata service
│   └── src/main/java/com/netflix/contentservice/
│       ├── model/
│       │   ├── Movie.java            # JPA entity (movies table)
│       │   ├── Genre.java            # Enum: ACTION, COMEDY, DRAMA, ...
│       │   └── VideoStatus.java      # Enum: PENDING → UPLOADED → ENCODING → READY
│       ├── dto/
│       │   ├── MovieRequest.java     # Input DTO with validation
│       │   └── MovieResponse.java    # API response DTO
│       └── controller/
│           └── content.java          # REST controller
│
├── encoding-service/                 # Video transcoding service
│   └── src/main/java/...             # Kafka consumer + S3 uploader
│
├── streaming-service/                # Playback URL service
│   └── src/main/java/...             # Kafka consumer + Redis cache
│
├── video-service/                    # Video upload service
│   └── src/main/java/...             # Kafka producer + S3 uploader
│
├── docker-compose.yml                # Full local environment
└── .env                              # AWS credentials & endpoint config
```

---

## Data Model

### Movie Entity (`content_db.movies`)

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `title` | String | Required |
| `description` | String | |
| `genre` | Genre (enum) | ACTION, COMEDY, DRAMA, HORROR, SCI_FI, ROMANCE, THRILLER, DOCUMENTARY |
| `director` | String | |
| `cast` | String | |
| `releaseYear` | int | |
| `rating` | double | |
| `thumbnailUrl` | String | |
| `durationInMinutes` | int | |
| `videoKey` | String | S3 object key for raw video |
| `hlsMasterPlaylistUrl` | String | S3 URL for encoded HLS playlist |
| `status` | VideoStatus (enum) | Lifecycle state |
| `createdAt` | Timestamp | Auto-managed |
| `updatedAt` | Timestamp | Auto-managed |

### VideoStatus Lifecycle

```
PENDING → UPLOADED → ENCODING → ENCODED → READY
                                        → FAILED
```

---

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17+
- Maven 3.9+
- AWS S3-compatible storage (or configure a local MinIO instance)

### Environment Configuration

Copy the `.env.example` (or create `.env`) with your S3 credentials:

```env
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_ENDPOINT_URL_S3=https://your-s3-endpoint
AWS_REGION=auto
```

### Run with Docker Compose

```bash
# Start all infrastructure and services
docker compose up --build

# Start only infrastructure (MySQL, Kafka, Redis)
docker compose up mysql-netflix kafka zookeeper redis-netflix
```

### Run a Single Service Locally

```bash
cd content-service
mvn spring-boot:run
```

---

## Key Design Decisions

**Event-Driven Architecture** — Services communicate exclusively through Kafka events. This decouples the video upload, encoding, and delivery pipeline so each stage can scale independently and failures in one service don't cascade.

**Database per Service** — content-service owns MySQL for structured metadata; encoding, streaming, and video services use S3 for binary data. No service shares another's database.

**Redis Caching for Streaming URLs** — HLS master playlist URLs are cached in Redis after encoding completes, so playback requests are served without hitting S3 or recomputing URLs on every request.

**S3-Compatible Storage** — Using a configurable S3 endpoint (`AWS_ENDPOINT_URL_S3`) allows swapping between AWS S3, MinIO, or any compatible store without code changes.

---

## Roadmap

- [ ] Implement Kafka producers in video-service
- [ ] Implement Kafka consumers and FFmpeg transcoding in encoding-service
- [ ] Implement REST endpoints in content-service controller
- [ ] Add Redis TTL strategy in streaming-service
- [ ] Add Spring Security / OAuth2 for authentication
- [ ] Add API Gateway (Spring Cloud Gateway)
- [ ] Add service discovery (Eureka / Consul)
- [ ] Add distributed tracing (Zipkin / OpenTelemetry)
- [ ] Add health checks and circuit breakers (Resilience4j)
- [ ] Add API documentation (Springdoc OpenAPI / Swagger UI)
- [ ] Migrate to Kubernetes for production orchestration
