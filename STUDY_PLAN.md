# Java Concurrency & PostgreSQL Scaling

*Study plan built around the chosen courses, books, and hands-on projects*

## How this plan works

Roughly 19 weeks at 6–8 hours a week, in three core phases plus an optional fourth. Each week pairs a small amount of learning material with a concrete coding deliverable. The coding is the point; the courses and books exist to support it.

- **One at a time:** one course, one book, and one coding project active at any moment.
- **Done means committed:** a week is finished when the code is in the repo, with tests, plus a few lines of takeaways in notes/weekNN.md on what you learned and what surprised you.
- **Weeks are flexible:** if a project runs long, let it. Cut learning material before cutting coding.
- **Bug katas:** week 1 is built around diagnosing and fixing naive code that Claude Code generates. You can ask for more katas in any later week to practise that week’s topic.
- **Short names:** courses, books, and tools are referred to by short names in the weekly sections. Full titles and links are in the appendix.

**Setup (one evening):** a Maven multi-module repo with JDK 25 (the current LTS), JMH, jcstress, Testcontainers, jOOQ, and Docker Compose for PostgreSQL. Add async-profiler and pgbench when needed.

## Phase 1: Concurrency (weeks 1–7)

*Main course: Pogrebinsky. Book: Java Concurrency in Practice. Theory supplement: Rice, Concurrent Programming in Java. Rice lecture numbers follow the Coursera course (modules 1–4); each module also has a mini-project you can do in addition to the weekly build. The module 4 mini-project (Boruvka’s minimum spanning tree) is optional. Lecture titles in modules 2 and 3 may differ slightly on Coursera; the numbers are what to match. Rice is not used after Phase 1.*

### Week 1: Bug katas — diagnose and fix naive concurrent code

**Focus:** atomicity, visibility, happens-before, reordering, safe publication; learning to recognise each failure mode in plausible-looking code.

**Learn:**
- Pogrebinsky: introductory and thread-fundamentals sections.
- JCIP ch. 2, 3, and 16.
- Rice, module 1: 1.1 Threads. (Rice does not cover the memory model in depth; JCIP ch. 16 is the main source this week.)

**Build:**
- Ask Claude Code to generate 6–8 bug katas in week01-katas: small, plausible, non-thread-safe classes, each with a stress-test harness that fails some of the time. Cover lost updates, check-then-act, a worker stopped by a non-volatile flag, unsafe lazy initialisation, unsafe publication, a shared non-thread-safe collection, and a compound action on a thread-safe collection. Claude Code does not reveal which failure each kata has.
- For each kata: run the test and watch it fail; before changing any code, write the diagnosis in a DIAGNOSIS.md in the kata’s package (which failure mode, which happens-before edge is missing or which sequence is not atomic); then fix it and re-run many times.
- Plain stress tests first: an executor, a CountDownLatch start gate so threads really overlap, many iterations, and assertions on invariants.
- Introduce jcstress only for the katas a plain test cannot show reliably (visibility and reordering): Termination mode for the stop flag, and an actor/arbiter test for unsafe publication or lazy initialisation. On x86 some reorderings may never appear; record in the notes why the code is still broken.
- Ask Claude Code to review both the diagnoses and the fixes.
- Write the week’s takeaways in notes/week01.md: patterns you noticed, surprises, and any diagnoses the review showed were wrong.

### Week 2: Locks, atomics, and your own primitives

**Focus:** intrinsic locks, wait/notify, ReentrantLock, conditions, CAS, contention.

**Learn:**
- Pogrebinsky: data sharing, locking, and atomic operations sections.
- JCIP ch. 5, 13, 14, and 15.
- Rice, module 1: 1.2 Structured Locks, 1.3 Unstructured Locks, and the Locking and Synchronization demonstration.
- Rice, module 2: 2.1 Critical Sections, 2.4 Atomic Variables, 2.5 Read-Write Isolation.
- Rice, module 3: 3.5 Bounded Buffer Problem (pairs with the bounded queue exercise).

**Build:**
- A bounded blocking queue, first with wait/notify, then with ReentrantLock and two Conditions.
- JMH benchmark: synchronized counter vs AtomicLong vs LongAdder across thread counts.
- JMH benchmark showing false sharing between two counters, fixed with padding (or @Contended).

### Week 3: Executors and liveness

**Focus:** thread pool internals, sizing, rejection, shutdown, deadlock.

**Learn:**
- JCIP ch. 6–8 and 10.
- Rice, module 1: 1.4 Liveness, 1.5 Dining Philosophers.
- Rice, module 2: 2.2 Object-Based Isolation (monitors). (Rice does not cover executors; JCIP ch. 6–8 is the source.)

**Build:**
- A minimal thread pool from scratch (workers, task queue, shutdown()/shutdownNow(), rejection policy); compare with ThreadPoolExecutor.
- Create a deadlock with two locks in opposite order, find it in a jstack dump, fix it with lock ordering.

### Weeks 4–5: CMU buffer pool manager, in Java

**Focus:** a real concurrent component: shared state, fine-grained latching, eviction.

**Learn:**
- CMU 15-445: lectures on database storage and buffer pools.
- The CMU buffer pool project specification (use it as the design spec).
- Rice, module 2: 2.3 the concurrent spanning tree example (fine-grained, per-object locking).
- Rice, module 4: 4.1 Optimistic Concurrency, 4.4 Concurrent Hash Map (relevant to the page table).

**Build:**
- Disk manager over FileChannel with fixed 4 KB pages held in ByteBuffers.
- LRU-K replacer, page table, pin counts, dirty-page flushing.
- Thread safety throughout: decide what each lock protects; avoid deadlock between the page table and per-page latches.
- Concurrent stress tests with many threads fetching, pinning, and evicting pages.

### Week 6: Pipelines and asynchronous composition

**Focus:** bounded queues, backpressure, ordering, shutdown, CompletableFuture.

**Learn:**
- Pogrebinsky: sections on inter-thread communication.
- Rice, module 3: 3.1 Actors, 3.2 Actor Examples, 3.3 Sieve of Eratosthenes, 3.4 Producer-Consumer Problem (actors are a useful contrast to a queue-based pipeline).
- Rice, module 4: 4.2 Concurrent Queue, 4.3 Linearizability.

**Build:**
- Producer–consumer pipeline over a large generated file: one reader, N workers, one writer, bounded queues, sequence numbers with a reorder buffer, poison-pill shutdown, per-record error handling.
- A CompletableFuture aggregator calling simulated slow services with timeouts and fallbacks.
- Profile the pipeline with async-profiler and fix its top bottleneck.

### Week 7: Virtual threads on JDK 25

**Focus:** where virtual threads help and where they hurt: I/O vs CPU-bound work, limited downstream resources, ThreadLocal cost, structured concurrency, Amdahl’s law.

**Learn:**
- Pogrebinsky: virtual threads and performance sections.
- JCIP ch. 11 and 12.
- Rice: nothing on virtual threads (the course is Java 8-era). Optional: Amdahl’s law is covered in module 1 of the first course in the series, Parallel Programming in Java.
- Optional: Kabutz, Mastering Virtual Threads in Java, if the topic still feels shaky.
- JEP 444 (virtual threads), JEP 491 (why synchronized no longer pins since Java 24), JEP 506 (scoped values), JEP 505 (structured concurrency, preview in 25).

**Build:**
- Load test with 10,000 concurrent blocking calls: platform thread pool vs virtual threads.
- The connection-pool trap: 10,000 virtual threads hitting PostgreSQL through a HikariCP pool of 10. Observe the waiting and timeouts, then limit concurrency with a Semaphore.
- CPU-bound tasks on virtual threads vs a platform pool sized to the cores; show there is no gain and observe carrier starvation.
- A million virtual threads with a heavy ThreadLocal: measure memory, then compare with ScopedValue.
- Fan out to several simulated services with StructuredTaskScope (--enable-preview), including cancellation and failure handling.
- Optional, 30 minutes: install JDK 21 with SDKMAN and watch the jdk.VirtualThreadPinned JFR event fire for blocking inside synchronized, to understand the issue still found in Java 21 codebases.

## Phase 2: Databases (weeks 8–15)

*Main course: Nasser. Book: PostgreSQL 14 Internals (plus DDIA ch. 7). Supplements: Postgres Professional materials, selected CMU lectures.*

### Week 8: Big data, query plans, indexes

**Focus:** EXPLAIN (ANALYZE, BUFFERS), the planner, B-tree, composite/covering/partial indexes, bulk loading.

**Learn:**
- Nasser: indexing sections.
- Rogov Part IV (ch. 16–20) and ch. 25.
- Postgres Professional: query performance course materials.

**Build:**
- Generate 100 million transactions with generate_series; measure typical queries before and after indexing.
- Bulk-load three ways (row by row, jOOQ batch, COPY) and compare throughput.

### Weeks 9–11: CMU B+ tree index, in Java

**Focus:** how an index really works, and concurrency inside a data structure.

**Learn:**
- CMU 15-445: lectures on tree indexes and index concurrency control.
- The CMU B+ tree project specification.

**Build:**
- B+ tree stored in pages from your week 4–5 buffer pool.
- Insert with node splits, delete with merges and redistribution, range iterator.
- Latch crabbing: release a parent’s latch once the child is known to be safe.
- Concurrent correctness tests, and a benchmark against ConcurrentSkipListMap.

### Week 12: Transactions and isolation

**Focus:** MVCC, isolation levels, anomalies, row locks, deadlocks, optimistic locking.

**Learn:**
- Nasser: ACID and concurrency control sections.
- Rogov Part I (ch. 2–7) and Part III (ch. 12–13).
- DDIA ch. 7.
- CMU: concurrency control and MVCC lectures (optional).

**Build:**
- Transfer service with jOOQ, driven by concurrent transfers from a Java executor.
- Show lost updates, then fix with FOR UPDATE, version columns, and a conditional update.
- Trigger and fix a deadlock between opposite-direction transfers.
- Reproduce write skew under Repeatable Read; handle it with Serializable plus retries.

### Week 13: Partitioning and job queues

**Focus:** range and hash partitioning, pruning, data lifecycle, SKIP LOCKED, outbox.

**Learn:**
- Nasser: partitioning sections.

**Build:**
- Convert the 100M-row table to monthly range partitions and measure pruning; detach and archive an old partition.
- Job queue using SKIP LOCKED with competing workers.
- Outbox table with a relay that publishes events exactly once.

### Week 14: Replication and connection pooling

**Focus:** streaming replication, lag, read-your-writes, HikariCP sizing, PgBouncer.

**Learn:**
- Nasser: replication sections.
- Rogov Part II (skim).
- Postgres Professional: replication-related materials.

**Build:**
- Primary plus streaming replica in Docker Compose; route reads to the replica and observe lag breaking read-your-writes.
- Find the right HikariCP pool size with pgbench and your own load test.
- PgBouncer in transaction mode: find what breaks.

### Week 15: Sharding

**Focus:** shard keys, consistent hashing, cross-shard operations, global IDs.

**Learn:**
- Nasser: sharding sections.

**Build:**
- Citus in Docker: distribute by account_id; compare co-located and cross-shard queries.
- Application-level router with consistent hashing over several PostgreSQL instances.
- Thread-safe Snowflake-style ID generator.

## Phase 3: Distributed systems and design (weeks 16–19)

*Book: Designing Data-Intensive Applications. Lectures: selected MIT 6.5840.*

### Weeks 16–17: Replication, consistency, and cross-shard work

**Focus:** consistency models, consensus, sagas, change data capture.

**Learn:**
- DDIA ch. 5, 6, 9, and 11 (ch. 7 already done).
- MIT 6.5840: lectures on Raft, linearizability, and Spanner.

**Build:**
- Cross-shard transfer as a saga with compensation, on top of the week-15 router.
- Idempotency keys end to end; failure injection to prove no double-spending.

### Weeks 18–19: Capstone

**Focus:** bringing everything together and telling the story.

**Learn:**
- Practice system design out loud: ledger, wallet, rate limiter, file-ingestion pipeline.

**Build:**
- One service: high-throughput ingestion (Phase 1 pipeline) into partitioned PostgreSQL, outbox, correct concurrent balance updates, load test.
- Profile, fix the top bottleneck, and write a one-page design summary of the trade-offs.
- Rapid-fire review of the Phase 1 fundamentals.

## Phase 4: Optional, later

- **Kabutz, Extreme Java – Concurrency Performance:** if you want more depth after finishing the book. (Skip the bundles; they overlap with Pogrebinsky.)
- **Rice, parallel and distributed courses:** the other two parts of the series.
- **MIT 6.5840 labs in Java:** MapReduce, then Raft, then a fault-tolerant key/value store. The biggest time investment on the list.
- **Remaining CMU projects:** query execution and concurrency control, if database internals turn out to be your thing.

## Appendix: Resources

*Course pages and URLs can change. If a link stops working, search by the full title.*

### Courses

- **Pogrebinsky:** *Java Multithreading, Concurrency & Performance Optimization* — Michael Pogrebinsky, Udemy. <https://www.udemy.com/course/java-multithreading-concurrency-performance-optimization/>
- **Rice:** *Concurrent Programming in Java* — Rice University (Vivek Sarkar), Coursera; part of the specialization Parallel, Concurrent, and Distributed Programming in Java. <https://www.coursera.org/learn/concurrent-programming-in-java>
- **Rice (full series):** *Parallel, Concurrent, and Distributed Programming in Java Specialization* — Rice University, Coursera. <https://www.coursera.org/specializations/pcdp>
- **Kabutz (optional, week 7):** *Mastering Virtual Threads in Java* — Dr Heinz M. Kabutz, JavaSpecialists; virtual threads, structured concurrency, scoped values, diagnosing deadlocks. Buy only if virtual threads still feel shaky at week 7. Listed under the Concurrency category. <https://javaspecialists.teachable.com/courses/category/Concurrency>
- **Kabutz (optional, later):** *Extreme Java – Concurrency Performance* — Dr Heinz M. Kabutz, JavaSpecialists; self-paced, about 14.5 hours, a modernised companion to Java Concurrency in Practice. Only if gaps remain after Phase 1. <https://javaspecialists.eu/courses/jpt/>
- **Kabutz (free):** *The Java Specialists’ Newsletter* — Dr Heinz M. Kabutz; long-running free archive with many concurrency articles. <https://www.javaspecialists.eu/archive/>
- **Nasser:** *Fundamentals of Database Engineering* — Hussein Nasser, Udemy. <https://www.udemy.com/course/database-engines-crash-course/>
- **Postgres Professional:** *PostgreSQL training courses (e.g. query performance tuning, administration)* — Postgres Professional; free slides and videos. <https://postgrespro.com/education/courses>
- **CMU 15-445:** *CMU 15-445/645: Intro to Database Systems* — Carnegie Mellon University (Andy Pavlo); lectures, notes, and project specifications. Lecture videos on the CMU Database Group YouTube channel. <https://15445.courses.cs.cmu.edu/>
- **MIT 6.5840:** *MIT 6.5840: Distributed Systems (formerly 6.824)* — MIT (Robert Morris); schedule, papers, lecture videos, and labs. <https://pdos.csail.mit.edu/6.824/>

### Books

- **JCIP:** *Java Concurrency in Practice* — Brian Goetz, Tim Peierls, Joshua Bloch, Joseph Bowbeer, David Holmes, Doug Lea; Addison-Wesley, 2006. <https://jcip.net/>
- **DDIA:** *Designing Data-Intensive Applications* — Martin Kleppmann; O’Reilly. Chapter numbers in this plan follow the first edition (2017); check whether the second edition is out. <https://dataintensive.net/>
- **Rogov:** *PostgreSQL 14 Internals* — Egor Rogov, Postgres Professional; free PDF. <https://postgrespro.com/community/books/internals>

### Tools and references

- **JMH:** *Java Microbenchmark Harness* — OpenJDK. <https://github.com/openjdk/jmh>
- **jcstress:** *Java Concurrency Stress tests* — OpenJDK. <https://github.com/openjdk/jcstress>
- **async-profiler:** *async-profiler* — sampling profiler for Java. <https://github.com/async-profiler/async-profiler>
- **JEP 444:** *JEP 444: Virtual Threads* — OpenJDK, final in Java 21. <https://openjdk.org/jeps/444>
- **JEP 491:** *JEP 491: Synchronize Virtual Threads without Pinning* — OpenJDK, Java 24. <https://openjdk.org/jeps/491>
- **JEP 505:** *JEP 505: Structured Concurrency (Fifth Preview)* — OpenJDK, Java 25. <https://openjdk.org/jeps/505>
- **JEP 506:** *JEP 506: Scoped Values* — OpenJDK, final in Java 25. <https://openjdk.org/jeps/506>
- **SDKMAN:** *SDKMAN!* — for installing and switching between JDK versions. <https://sdkman.io/>
- **Testcontainers:** *Testcontainers for Java*. <https://java.testcontainers.org/>
- **jOOQ:** *jOOQ* — typesafe SQL for Java. <https://www.jooq.org/>
- **HikariCP:** *HikariCP* — JDBC connection pool; see its wiki page “About Pool Sizing”. <https://github.com/brettwooldridge/HikariCP>
- **PgBouncer:** *PgBouncer* — lightweight PostgreSQL connection pooler. <https://www.pgbouncer.org/>
- **pgbench:** *pgbench* — PostgreSQL benchmarking tool (PostgreSQL docs). <https://www.postgresql.org/docs/current/pgbench.html>
- **Citus:** *Citus* — distributed PostgreSQL extension. <https://github.com/citusdata/citus>
