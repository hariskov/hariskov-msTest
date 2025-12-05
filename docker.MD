# Phase 1: Docker Fundamentals ✅

## Overview

This phase demonstrates containerization of a distributed database setup: one Spring Boot application connecting to
three PostgreSQL shards. This is the foundation for learning database sharding and microservices architecture.

## Architecture

```
┌─────────────────────┐
│  user-management    │
│  (Spring Boot)      │
│  Port: 8080         │
└──────────┬──────────┘
           │
    ┌──────┴──────┬──────────┐
    │             │          │
┌───▼────┐   ┌───▼────┐ ┌───▼────┐
│ shard-0│   │ shard-1│ │ shard-2│
│ :5432  │   │ :5433  │ │ :5434  │
└────────┘   └────────┘ └────────┘
```

## Docker Compose Configuration

### Key Design Decisions

**Three Separate Database Shards:**

- Each PostgreSQL instance runs in its own container
- Separate named volumes ensure data isolation
- Different host ports (5432, 5433, 5434) allow external connections
- All use port 5432 internally (PostgreSQL default)

**Health Checks:**

- Application waits for all databases to be healthy before starting
- Prevents connection errors during startup
- `pg_isready` checks PostgreSQL is accepting connections

**Service Naming:**

- Service names (`postgres-shard-0`) used for internal Docker networking
- Spring Boot connects using service names, not `localhost`
- Container names (`user-db-shard-0`) for human-readable `docker ps` output

### Full Configuration

```yaml
services:
  postgres-shard-0:
    build:
      context: ../../..
      dockerfile: src/main/docker/db/Dockerfile
    container_name: user-db-shard-0
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: shard0db
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U postgres" ]
      interval: 10s
      timeout: 5s
      retries: 5
    ports:
      - "5432:5432"  # HOST:CONTAINER format
    volumes:
      - shard0-data:/opt/msTest/data

  postgres-shard-1:
    build:
      context: ../../..
      dockerfile: src/main/docker/db/Dockerfile
    container_name: user-db-shard-1
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: shard1db
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U postgres" ]
      interval: 10s
      timeout: 5s
      retries: 5
    ports:
      - "5433:5432"  # Different host port, same container port
    volumes:
      - shard1-data:/opt/msTest/data

  postgres-shard-2:
    build:
      context: ../../..
      dockerfile: src/main/docker/db/Dockerfile
    container_name: user-db-shard-2
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: shard2db
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U postgres" ]
      interval: 10s
      timeout: 5s
      retries: 5
    ports:
      - "5434:5432"
    volumes:
      - shard2-data:/opt/msTest/data

  user-management:
    build:
      context: ../../..
      dockerfile: src/main/docker/deployment/Dockerfile
    container_name: user-management-app
    depends_on:
      postgres-shard-0:
        condition: service_healthy
      postgres-shard-1:
        condition: service_healthy
      postgres-shard-2:
        condition: service_healthy
    ports:
      - "8080:8080"

volumes:
  shard0-data:
  shard1-data:
  shard2-data:
```

## Essential Docker Commands

### Lifecycle Management

```bash
# Start all services (creates images/volumes if needed, reuses existing ones)
docker compose up

# Start in detached mode (runs in background)
docker compose up -d

# Stop and remove containers + networks
docker compose down

# Stop and remove containers + networks + volumes (⚠️ destroys data)
docker compose down -v

# Rebuild images before starting (useful after Dockerfile changes)
docker compose up --build
```

### Debugging & Inspection

```bash
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# View logs from a container
docker logs user-db-shard-0

# Follow logs in real-time
docker logs -f user-db-shard-0

# Attach to container (interactive mode, Ctrl+C to exit)
docker attach user-management-app

# Execute command in running container
docker exec -it user-db-shard-0 psql -U postgres -d shard0db

# Inspect container details (networking, volumes, etc.)
docker inspect user-db-shard-0
```

### Volume Management

```bash
# List all volumes
docker volume ls

# Inspect volume details (see where it's stored on host)
docker volume inspect docker_shard0-data

# Remove specific volume (⚠️ destroys data)
docker volume rm docker_shard0-data

# Remove all unused volumes
docker volume prune
```

### Network Inspection

```bash
# List networks
docker network ls

# Inspect network (see which containers are connected)
docker network inspect docker_default

# See container IP addresses
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' user-db-shard-0
```

## Core Concepts Mastered

### 1. Docker Networking

**Internal Container Communication:**

- Containers on the same Docker Compose network use service names for DNS resolution
- Spring Boot connects to `postgres-shard-0:5432`, not `localhost:5432`
- Docker's internal DNS translates `postgres-shard-0` to the container's IP
- This works whether Spring Boot is containerized or running on host

**External Host Communication:**

- Port mapping `5432:5432` exposes container port 5432 to host port 5432
- From host machine: `psql -h localhost -p 5432` connects to shard-0
- From host machine: `psql -h localhost -p 5433` connects to shard-1
- The exposed ports are for developer access, not inter-container communication

**Critical Understanding:**

- `localhost` inside a container refers to **that container**, not the host machine
- Service names (`postgres-shard-0`) work for container-to-container communication
- Port mappings (`5432:5432`) are for host-to-container communication

### 2. Data Persistence with Volumes

**Named Volumes (What We Use):**

```yaml
volumes:
  shard0-data:  # Declaration at root level

services:
  postgres-shard-0:
    volumes:
      - shard0-data:/opt/msTest/data  # Usage in service
```

**Key Properties:**

- Created independently of containers
- Survive `docker compose down` (without `-v` flag)
- Can be reused across different containers
- Stored in Docker-managed location on host
- Accessed by name, easy to manage

**Anonymous Volumes:**

- Created automatically with random IDs
- Harder to reuse (need to remember the ID)
- Generally avoid for data you care about

**Data Lifecycle:**

- Container restart: Data persists ✅
- Container removal: Data persists ✅
- `docker compose down`: Data persists ✅
- `docker compose down -v`: Data deleted ❌

### 3. Images vs Containers

**Images:**

- Blueprint/template for containers
- Immutable - never change once built
- Downloaded once from Docker Hub, cached locally
- Shared across multiple container instances
- Built from Dockerfile

**Containers:**

- Running instance of an image
- Ephemeral - can be stopped, started, removed
- Have their own filesystem, network, processes
- Each container is isolated from others
- Created from images

**Example:**

```
postgres:15 image → Creates three separate containers
                    ├─ user-db-shard-0
                    ├─ user-db-shard-1
                    └─ user-db-shard-2
```

### 4. Health Checks

```yaml
healthcheck:
  test: [ "CMD-SHELL", "pg_isready -U postgres" ]
  interval: 10s
  timeout: 5s
  retries: 5
```

**What This Does:**

- Runs `pg_isready` every 10 seconds
- If command takes >5 seconds, marks as timeout
- Must succeed 5 times before marking healthy
- Other containers can wait for healthy status with `depends_on`

**Why It Matters:**

- Prevents race conditions (app starting before database is ready)
- Better than sleep timers (waits exactly as long as needed)
- Provides visibility into container health

### 5. Port Mapping Format

**Format: `HOST:CONTAINER`**

```yaml
ports:
  - "5433:5432"
```

This means:

- Container port: `5432` (PostgreSQL listens here inside container)
- Host port: `5433` (you connect to this from your laptop)
- PostgreSQL doesn't know or care about the host port

**Why Different Host Ports:**

- Can't have three containers all using host port 5432
- Each needs unique host port: 5432, 5433, 5434
- All still use 5432 internally (standard PostgreSQL port)

## What I Learned

### Aha Moments

**1. Networking Mental Model:**
When Spring Boot runs on my laptop and connects to `localhost:5432`, it goes through my laptop's network to a port that
Docker mapped. When containerized Spring Boot connects to `postgres-shard-0:5432`, it uses Docker's internal network
directly. The first adds a network hop, the second is the "Docker way."

**2. Volume Persistence:**
Volumes exist independently of containers. I can destroy and recreate containers all day long, and my database data
survives because it lives in the volume. This is why named volumes are critical for databases.

**3. Port Mapping Direction:**
I initially had `5432:5433` thinking it meant "container 5432 maps to host 5433" but it's backwards: `HOST:CONTAINER`.
This clicked when I realized you're always configuring from the host's perspective.

**4. Health Checks Enable Orchestration:**
Without health checks, `depends_on` only waits for container to start, not for PostgreSQL to be ready. Health checks let
you express "don't start my app until databases are actually accepting connections."

### Challenges Solved

**Challenge: Understanding `localhost` in containers**
Initially tried to connect Spring Boot container to `localhost:5432` and got connection refused. Realized `localhost`
inside a container is that container's loopback, not the host machine. Solution: use service names.

**Challenge: Data disappearing after container restart**
First setup had no volumes. Every `docker compose down` lost all data. Learned that container filesystem is ephemeral,
volumes are for persistence.

**Challenge: Port conflicts**
Tried to run three databases all on host port 5432. Docker rejected it. Learned that host ports must be unique, but
container ports can be the same across different containers.

## Key Takeaways

### When Docker Matters

- **Consistency**: "Works on my machine" becomes "works in this container" = works everywhere
- **Isolation**: Run multiple PostgreSQL versions/instances without conflicts
- **Rapid setup**: New team member runs `docker compose up`, they have full environment
- **Production parity**: Same containers in dev, staging, production

### Trade-offs Understood

- **Overhead**: Containers add slight resource overhead vs native processes
- **Complexity**: Need to learn Docker concepts and commands
- **Networking**: Extra layer of networking abstraction to understand
- **Debugging**: Slightly harder to debug than native processes (but not much)

### Production Considerations

- Never hardcode passwords in docker-compose.yml (use secrets/env files)
- Use specific image tags (`postgres:15.3`) not `latest` for reproducibility
- Consider resource limits for containers in production
- Health checks are critical for orchestration and monitoring
- Named volumes for any data you care about

## Questions I Still Have

**For Phase 2:**

- How does Spring Boot actually connect to multiple datasources?
- What's the performance impact of querying across shards?
- How do I decide which shard to query for a given request?

**For Phase 5 (Kubernetes):**

- How is Kubernetes networking different from Docker Compose?
- How do volumes work in Kubernetes vs Docker?
- What happens to my containers when I move to K8s?

## Verification

To confirm Phase 1 completion:

```bash
# All three databases running
docker ps | grep user-db-shard

# Can connect to each shard
psql -h localhost -p 5432 -U postgres -d shard0db -c '\l'
psql -h localhost -p 5433 -U postgres -d shard1db -c '\l'
psql -h localhost -p 5434 -U postgres -d shard2db -c '\l'

# Three separate volumes exist
docker volume ls | grep shard

# Application container running and healthy
docker ps | grep user-management-app
```

## Next Steps

**Phase 2: Database Sharding**

- Configure Spring Boot to connect to all three shards
- Implement ShardRouter to decide which database to query
- Experience the trade-offs of distributed data
- Understand when sharding helps vs hurts

---

**Phase 1 Status: ✅ Complete**  
**Time Invested:** ~8 hours  
**Next Phase:** Database Sharding (Week 3)