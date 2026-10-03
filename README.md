# java-concurrency-postgres-lab

Hands-on self-study of Java concurrency and PostgreSQL scaling: build it, break it,
measure it. Each week has a small amount of reading and a concrete coding deliverable.
The full plan, with resources, is in [`STUDY_PLAN.md`](STUDY_PLAN.md).

## Contents

| Phase | Weeks | Focus |
|---|---|---|
| 1. Concurrency | 1–7 | Memory model, locks and atomics, executors, a buffer pool manager, pipelines, virtual threads on JDK 25 |
| 2. Databases | 8–15 | Query plans and indexes, a concurrent B+ tree, isolation and locking, partitioning, replication, sharding |
| 3. Distributed systems and design | 16–19 | Consistency, sagas, capstone service |

## Layout

```
STUDY_PLAN.md     weekly goals and resources
CLAUDE.md         instructions for Claude Code (reviewer, not author)
common/           shared test utilities and scaffolding
weekNN-topic/     one module per week or project
docker/           PostgreSQL, replica, PgBouncer, Citus setups
notes/weekNN.md   what I learned each week
```

## Requirements

- JDK 25 (LTS)
- Maven
- Docker (for PostgreSQL and Testcontainers)

## Running

```bash
mvn verify                                   # unit and stress tests
mvn -pl week01-jmm verify -Pjcstress         # jcstress tests for a module
mvn -pl week02-primitives package -Pjmh && \
  java -jar week02-primitives/target/benchmarks.jar   # JMH benchmarks
docker compose -f docker/postgres.yml up -d  # local PostgreSQL
```

Profile names and paths may change as the scaffolding evolves; see each module's POM.

## Progress

- [ ] Week 1: The memory model, by breaking things
- [ ] Week 2: Locks, atomics, and your own primitives
- [ ] Week 3: Executors and liveness
- [ ] Weeks 4–5: Buffer pool manager
- [ ] Week 6: Pipelines and asynchronous composition
- [ ] Week 7: Virtual threads on JDK 25
- [ ] Week 8: Query plans and indexes
- [ ] Weeks 9–11: Concurrent B+ tree
- [ ] Week 12: Transactions and isolation
- [ ] Week 13: Partitioning and job queues
- [ ] Week 14: Replication and connection pooling
- [ ] Week 15: Sharding
- [ ] Weeks 16–17: Consistency and cross-shard work
- [ ] Weeks 18–19: Capstone
