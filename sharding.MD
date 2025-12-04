# Phase 2: Database Sharding ✅

## Overview

This phase demonstrates horizontal database scaling through sharding - splitting data across multiple database
instances. The goal is to understand what happens when a single database can't handle the load, and more importantly, to
experience the significant trade-offs that come with data distribution.

## Architecture

```
┌─────────────────────────────┐
│    Spring Boot Service      │
│      (User Service)         │
└──────────┬──────────────────┘
           │
           │ ShardRouter
           │ (determines shard by user ID hash)
           │
    ┌──────┴────────┬──────────┐
    │               │          │
┌───▼─────┐   ┌────▼────┐ ┌───▼─────┐
│ Shard 0 │   │ Shard 1 │ │ Shard 2 │
│ :5432   │   │ :5433   │ │ :5434   │
│ Users:  │   │ Users:  │ │ Users:  │
│ A,D,G...│   │ B,E,H...│ │ C,F,I...│
└─────────┘   └─────────┘ └─────────┘
```

## What is Database Sharding?

**Definition:** Sharding is the process of splitting data horizontally across multiple database instances (shards),
where each shard contains a subset of the total data.

**The Problem It Solves:**

- Single database hits physical limits (storage, memory, CPU)
- Write throughput exceeds what one database can handle
- Data volume grows beyond vertical scaling capabilities

**Example Scenario:**

- Start: 1,000 users → Single PostgreSQL database (fast, simple)
- Growth: 10 million users → Database struggles with load
- Scale: 100 million users → Single database physically can't handle it
- Solution: Split users across 3 shards → Each shard handles ~33 million users

## Implementation Details

### Shard Key Selection

**Critical Decision:** What attribute determines which shard holds the data?

**We chose: User ID (UUID)**

```java
public int determineShardIndex(String userId) {
    int hash = userId.hashCode();
    return Math.abs(hash % 3);  // Distribute across 3 shards
}
```

**Why User ID:**

- ✅ Unique (no collisions)
- ✅ Immutable (never changes, stable placement)
- ✅ Even distribution (hash function spreads uniformly)
- ✅ Natural primary key for most queries

**Rejected Alternative: User Name**

- ❌ Not unique (multiple "John Smith")
- ❌ Can change (marriage, legal name change → requires data migration)
- ❌ Uneven distribution ("Smith", "Wang", "Garcia" are very common)
- ❌ Would make ID-based queries require scanning all shards

### Core Components

#### 1. ShardRouter

**Purpose:** Central authority that determines data placement

```java

@Component
public class ShardRouter {
    private final Map<Integer, DataSource> shards;

    public int determineShardIndex(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("Shard key cannot be null");
        }
        return Math.abs(userId.hashCode() % shards.size());
    }

    public DataSource getShard(int index) {
        DataSource ds = shards.get(index);
        if (ds == null) {
            throw new IllegalStateException("No shard found: " + index);
        }
        return ds;
    }
}
```

**Design Decision:** Fail loudly if routing logic breaks. No default datasource to prevent silent data misplacement.

#### 2. UserShardedRepository

**Purpose:** Data access layer that routes queries to correct shards

```java

@Repository
public class UserShardedRepository {

    // Query specific shard
    public UserEntity findById(String userId, DataSource ds) {
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        return jdbc.queryForObject(
                "SELECT id, name, shard_index FROM users WHERE id = ?",
                userRowMapper(),
                userId
        );
    }

    // Save to specific shard
    public UserEntity save(UserEntity user, DataSource ds) {
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.update(
                "INSERT INTO users (id, name, shard_index) VALUES (?, ?, ?)",
                user.getId(), user.getName(), user.getShardIndex()
        );
        return user;
    }
}
```

**Why JdbcTemplate instead of Spring Data JPA:**

- Spring Data repositories assume single datasource at startup
- Sharding requires dynamic datasource selection at runtime
- Trade-off: Lose abstraction, gain control

#### 3. UserService

**Purpose:** Business logic that coordinates sharding

```java

@Service
public class UserService {

    // Fast: Queries ONE shard
    public UserEntity getUserById(String userId) {
        int shard = shardRouter.determineShardIndex(userId);
        DataSource ds = shardRouter.getShard(shard);
        return repository.findById(userId, ds);
    }

    // Slow: Queries ALL shards
    public List<UserEntity> getUsersByName(String name) {
        List<UserEntity> results = new ArrayList<>();
        for (int i = 0; i < shardRouter.getShardCount(); i++) {
            DataSource ds = shardRouter.getShard(i);
            results.addAll(repository.findByName(name, ds));
        }
        return results;
    }

    // Creates and routes to correct shard
    public UserEntity createUser(String name) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setName(name);

        int shard = shardRouter.determineShardIndex(user.getId());
        user.setShardIndex(shard);

        DataSource ds = shardRouter.getShard(shard);
        return repository.save(user, ds);
    }
}
```

### Database Schema

**Each shard has identical schema:**

```sql
CREATE TABLE users (
    id VARCHAR PRIMARY KEY,
    name VARCHAR NOT NULL,
    shard_index INTEGER NOT NULL  -- Which shard this row belongs to
);
```

**Why store shard_index in each row:**

- Enables detection of data misplacement
- Audit queries: `SELECT * FROM users WHERE shard_index != 2` (in shard-2)
- Helps with debugging and data verification
- Supports data migration if rebalancing shards

## Query Performance Analysis

### Fast Queries (By Shard Key)

**Query by User ID:**

```java
UserEntity user = userService.getUserById("abc-123");
```

**What happens:**

1. Calculate shard: `hash("abc-123") % 3 = 1`
2. Query only shard 1
3. Return result

**Performance:** O(1) - Single database query  
**Scalability:** Excellent - Queries don't get slower as you add shards

**Real-world timing (observed):**

- Query by ID: ~5-10ms (hits one shard)

### Slow Queries (By Non-Shard Key)

**Query by Name:**

```java
List<UserEntity> users = userService.getUsersByName("John Doe");
```

**What happens:**

1. Don't know which shard has "John Doe"
2. Query shard 0 for "John Doe"
3. Query shard 1 for "John Doe"
4. Query shard 2 for "John Doe"
5. Combine results from all three shards

**Performance:** O(N) where N = number of shards  
**Scalability:** Poor - Gets slower as you add more shards

**Real-world timing (observed):**

- Query by name: ~25-35ms (hits all three shards)
- With 100 shards: Would take ~500ms+

### Impossible Queries (Cross-Shard Operations)

**Sort all users by name:**

```java
public List<UserEntity> getAllUsersSortedByName() {
    // Can't use SQL ORDER BY across shards
    List<UserEntity> allUsers = new ArrayList<>();

    // 1. Query all shards
    for (int i = 0; i < shardCount; i++) {
        allUsers.addAll(queryShardForAllUsers(i));
    }

    // 2. Load everything into memory
    // 3. Sort in application layer
    allUsers.sort(Comparator.comparing(UserEntity::getName));

    return allUsers;
}
```

**Problems:**

- Must load ALL data into memory
- Cannot paginate efficiently
- With millions of users, this is impossible

**Count all users:**

```java
public long countAllUsers() {
    long total = 0;
    for (int i = 0; i < shardCount; i++) {
        total += queryShardForCount(i);
    }
    return total;
}
```

**Problems:**

- Simple `COUNT(*)` now requires N queries
- In monolith: 1 query
- In sharded system: N queries (where N = shard count)

**Join across shards:**

```sql
-- This is basically impossible in sharded systems
SELECT u.name, o.total 
FROM users u 
JOIN orders o ON u.id = o.user_id
WHERE o.status = 'completed';
```

**Problems:**

- Users and Orders might be on different shards
- Cannot JOIN data that lives on different servers
- Must fetch data and JOIN in application layer

## Trade-offs Experienced

### What We Gained

**Horizontal Scalability:**

- Can add more shards to handle more data
- Each shard handles fraction of total load
- No single database bottleneck

**Data Isolation:**

- Different users' data physically separated
- Can be useful for compliance (data residency)
- Failure of one shard doesn't affect others

### What We Lost (Significant)

**Query Flexibility:**

- ❌ Queries by non-shard keys require scanning all shards
- ❌ Sorting across shards requires application-layer sorting
- ❌ Aggregations (COUNT, SUM, AVG) require querying all shards
- ❌ Full-text search becomes extremely complex

**Abstraction Layer:**

- ❌ Can't use Spring Data JPA repositories
- ❌ Must write manual SQL with JdbcTemplate
- ❌ Lost database-agnostic query layer
- ❌ More boilerplate code to maintain

**Operational Complexity:**

- ❌ Multiple databases to manage (backup, restore, monitor)
- ❌ Schema migrations must run on all shards
- ❌ Rebalancing shards requires massive data migration
- ❌ Debugging is harder (which shard has the data?)

**Development Velocity:**

- ❌ Every feature requires shard consideration
- ❌ Simple queries become complex
- ❌ Team must understand sharding strategy
- ❌ New developers have steeper learning curve

**Data Integrity:**

- ❌ Transactions can't span shards
- ❌ Foreign keys can't reference other shards
- ❌ Must handle consistency at application layer
- ❌ Risk of data ending up in wrong shard

## When to Shard (Almost Never)

### Exhaust These Options First

**1. Vertical Scaling (Bigger Server)**

- Easiest solution
- Modern servers can handle massive loads
- AWS RDS: Up to 128 vCPUs, 4TB RAM
- Try this first, always

**2. Read Replicas**

- Master handles writes
- Multiple replicas handle reads
- Scales read-heavy workloads (most applications)
- Much simpler than sharding

**3. Caching (Redis/Memcached)**

- Cache frequent queries
- Reduce database load by 80-90%
- Easier to implement than sharding

**4. Database Optimization**

- Add indexes
- Optimize slow queries
- Partition tables (not same as sharding)
- Often solves performance issues

**5. Archive Old Data**

- Move historical data to separate database
- Keep "hot" data in main database
- Simpler than sharding

**6. Separate Concerns**

- Analytics queries → Separate analytics database
- Reporting → Data warehouse
- Keep transactional database focused

### Shard Only When

You've truly exhausted all alternatives AND:

- ✅ Single database hit absolute physical limits
- ✅ Billions of rows that can't fit on one server
- ✅ Write throughput exceeds single database capability
- ✅ Have clear, stable shard key (user_id, tenant_id)
- ✅ Most queries use the shard key
- ✅ Team has expertise to manage complexity
- ✅ Budget for operational overhead

**Real Companies That Actually Need Sharding:**

- Instagram/Facebook (billions of users)
- Twitter (hundreds of millions of tweets daily)
- Slack (millions of independent workspaces)
- GitHub (hundreds of millions of repositories)

**Most Companies Don't Need Sharding:**

- Even with millions of users
- Modern databases + optimization + caching handle it
- The complexity cost exceeds the benefit

## Alternative Approaches

### Multi-Tenant Sharding (More Common)

Instead of sharding by user, shard by tenant (company/workspace):

```java
// Shard by company ID
int shard = hash(companyId) % shardCount;
```

**Advantages:**

- All queries are scoped to tenant anyway
- "Show me Company A's data" hits one shard
- Natural isolation for security/compliance
- Easier to reason about

**Examples:**

- Slack: Shard by workspace_id
- Shopify: Shard by store_id
- Salesforce: Shard by organization_id

### Database-Native Solutions

**CockroachDB:**

- Looks like PostgreSQL
- Automatically handles sharding internally
- Transparent to application
- Trade-off: Different consistency model

**MongoDB:**

- Built-in sharding
- Automatic rebalancing
- Trade-off: NoSQL, different query model

**Vitess (MySQL):**

- Sharding middleware for MySQL
- Used by YouTube, Slack
- Trade-off: Additional infrastructure layer

### Hybrid Approach

**Primary Database + Search Engine:**

- PostgreSQL (sharded): Source of truth for user data
- Elasticsearch: Fast search by any attribute
- Keep them synchronized via event stream
- Best of both worlds (but complex)

## Key Learnings

### Technical Insights

1. **Shard key is destiny** - Everything else becomes expensive
2. **Distributed data = distributed complexity** - No free lunch
3. **Abstractions break** - Must drop down to lower-level APIs
4. **Every query needs shard awareness** - Impacts entire codebase

### Architectural Insights

1. **Sharding is a last resort** - Only when scale demands it
2. **Operational cost is high** - Multiple databases to manage
3. **Team knowledge burden** - Everyone must understand sharding
4. **Development velocity decreases** - Everything becomes harder

### When Sharding Makes Sense

- Massive scale (billions of entities)
- Natural partitioning (multi-tenant)
- Most queries by shard key
- Team has distributed systems expertise
- Budget for complexity

### When Sharding Doesn't Make Sense

- Can scale vertically
- Read replicas solve the problem
- Complex query requirements
- Small team without expertise
- Premature optimization

## Production Considerations

### Data Safety Mechanisms

**1. Explicit Shard Index in Rows:**

```java
user.setShardIndex(calculatedShard);
```

- Enables verification: Is this row in correct shard?
- Audit query: `SELECT * FROM users WHERE shard_index != 0` (in shard-0)

**2. No Default Datasource:**

```java
// Fails loudly if routing logic breaks
if(datasource ==null){
        throw new

IllegalStateException("No shard found!");
}
```

- Prevents silent data misplacement
- Better to crash than corrupt data placement

**3. Logging All Routing Decisions:**

```java
log.info("Routing user {} to shard {}",userId, shardIndex);
```

- Helps debug data placement issues
- Audit trail of where data went

### Shard Rebalancing

**Problem:** Adding new shard requires data migration

**Initial: 3 shards**

- User ABC → Shard 0
- User DEF → Shard 1
- User GHI → Shard 2

**After adding 4th shard:**

- Hash changes: ABC might now map to Shard 3
- Must physically move data
- Requires downtime or complex migration strategy

**Strategies:**

- Consistent hashing (minimizes data movement)
- Double-write during migration
- Accept temporary inconsistency

### Schema Migrations

**Challenge:** Changes must apply to all shards

**Bad approach:**

```sql
-- Manually run on each shard
ALTER TABLE users ADD COLUMN email VARCHAR;
```

**Better approach:**

```java
// Migration script that runs on all shards
for(DataSource shard :allShards){

executeOnShard(shard, "ALTER TABLE users ADD COLUMN email VARCHAR");
}
```

**Production approach:**

- Use migration tools (Flyway, Liquibase)
- Apply to all shards atomically
- Have rollback plan

## Comparison: Monolith vs Sharded

| Aspect                        | Monolith                  | Sharded                 |
|-------------------------------|---------------------------|-------------------------|
| **Query by ID**               | Fast (single query)       | Fast (single shard)     |
| **Query by other attributes** | Fast (single query)       | Slow (all shards)       |
| **Sorting**                   | Fast (database ORDER BY)  | Slow (app-layer sort)   |
| **Aggregations**              | Fast (database COUNT/SUM) | Slow (combine from all) |
| **Joins**                     | Fast (database JOIN)      | Impossible              |
| **Transactions**              | ACID across all data      | Only within shard       |
| **Development**               | Simple, fast iteration    | Complex, slower         |
| **Operations**                | One database to manage    | N databases to manage   |
| **Scaling**                   | Vertical only             | Horizontal              |
| **Max Data Size**             | Single server limits      | Effectively unlimited   |

## Testing & Verification

### Test Scenarios Completed

**1. Create User - Verify Distribution**

```bash
curl -X POST "http://localhost:8080/users?name=Alice"
# Response: {"id":"abc","name":"Alice","shardIndex":1}

curl -X POST "http://localhost:8080/users?name=Bob"  
# Response: {"id":"def","name":"Bob","shardIndex":0}

curl -X POST "http://localhost:8080/users?name=Charlie"
# Response: {"id":"ghi","name":"Charlie","shardIndex":2}
```

**Observation:** Users distributed across shards based on ID hash

**2. Query by ID - Fast Path**

```bash
curl "http://localhost:8080/users/abc"
# Timing: ~5-10ms
# Hits: Only shard 1
```

**3. Query by Name - Slow Path**

```bash
curl "http://localhost:8080/users/search?name=Alice"
# Timing: ~25-35ms  
# Hits: All 3 shards
```

**Observation:** 3x slower because queries all shards

**4. Data Distribution Check**

- Created 6 users
- Distribution: 2 per shard (even distribution)
- Confirms hash function works well

### Performance Observations

| Operation       | Shards Hit | Time (ms) | Notes          |
|-----------------|------------|-----------|----------------|
| Create user     | 1          | ~8        | Fast           |
| Get by ID       | 1          | ~5-10     | Fast           |
| Search by name  | 3          | ~25-35    | 3x slower      |
| Count all users | 3          | ~30       | Must query all |

**With 100 shards:**

- Get by ID: ~5-10ms (unchanged)
- Search by name: ~500ms+ (scales linearly)

## What We Built

### File Structure

```
src/
├── config/
│   ├── ShardDataSourceConfig.java    # Datasource bean definitions
│   └── ShardRouter.java               # Shard routing logic
├── repository/
│   └── UserShardedRepository.java     # Data access with JdbcTemplate
├── service/
│   └── UserService.java               # Business logic with sharding awareness
├── entity/
│   └── UserEntity.java                # Plain POJO (no JPA annotations)
└── controller/
    └── UserController.java            # REST endpoints

resources/
├── application.yml                     # Three datasource configurations
└── db/
    └── schema.sql                     # Identical schema for all shards
```

### Key Code Patterns

**Pattern 1: Explicit Datasource Passing**

```java
// Repository accepts datasource, doesn't assume one
public UserEntity findById(String id, DataSource ds) {
    JdbcTemplate jdbc = new JdbcTemplate(ds);
    // ...
}
```

**Pattern 2: Service-Level Routing**

```java
// Service determines shard, passes correct datasource
public UserEntity getUserById(String id) {
    int shard = shardRouter.determineShardIndex(id);
    DataSource ds = shardRouter.getShard(shard);
    return repository.findById(id, ds);
}
```

**Pattern 3: Scatter-Gather for Non-Key Queries**

```java
// Query all shards, combine results
public List<UserEntity> getUsersByName(String name) {
    List<UserEntity> results = new ArrayList<>();
    for (int i = 0; i < shardCount; i++) {
        DataSource ds = shardRouter.getShard(i);
        results.addAll(repository.findByName(name, ds));
    }
    return results;
}
```

## Conclusion

### What Phase 2 Taught Us

**Technical Lesson:**
Sharding works mechanically - we successfully distributed data across multiple databases and can query it. The
implementation is straightforward.

**Architectural Lesson:**
Sharding is **almost always a bad idea** until scale absolutely demands it. The operational complexity, development
overhead, and query limitations make it a last resort, not a best practice.

**Real-World Insight:**
Most companies never need sharding. Those that do (Instagram, Facebook, Twitter) have billions of entities and teams of
distributed systems engineers. For everyone else, vertical scaling + read replicas + caching is sufficient.

### Key Takeaway

> **"Sharding is trading query flexibility and development velocity for horizontal scalability. Make this trade only
when you must, not because you can."**

### Skills Acquired

- ✅ Understand what sharding is and how it works
- ✅ Can implement hash-based sharding with Spring Boot
- ✅ Recognize performance implications of shard key choice
- ✅ Appreciate operational complexity of distributed data
- ✅ Know when sharding is necessary vs premature
- ✅ Can discuss sharding trade-offs in architecture discussions
- ✅ Understand why abstractions (Spring Data JPA) break with sharding

### Questions for Future Exploration

**For Phase 4 (Distributed Transactions):**

- How do we handle transactions that span shards?
- What is the Saga pattern and how does it help?
- How do we maintain consistency across shards?

**For Production Systems:**

- How do companies handle shard rebalancing?
- What's the migration path from monolith to sharded?
- How do you test sharded systems effectively?

---

**Phase 2 Status: ✅ Complete**  
**Time Invested:** ~6-8 hours  
**Next Phase:** Microservices Architecture (Weeks 4-5)

### Moving Forward

We've learned about **distributed data** (sharding). Phase 3 will teach us about **distributed services** (
microservices) - decomposing a monolith into separate services. We'll encounter similar trade-offs: losing simplicity
for organizational scalability.
