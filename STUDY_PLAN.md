# Java Concurrency & PostgreSQL Scaling

*Study plan built around the chosen courses, books, and hands-on projects*

## How this plan works

Roughly 21 weeks at 6–8 hours a week, in three core phases plus an optional fourth. Each week pairs a small amount of learning material with a concrete coding deliverable. The coding is the point; the courses and books exist to support it.

- **Main and second sources:** each week lists main sources, which you work through, and second explanations: other authors covering the same topic from a different angle. Use the second explanations to reinforce or clarify; they are worth reading, but the main sources come first if time is short.
- **One thing at a time:** one main course, one main book, and one coding project in progress at any moment.
- **Done means committed:** a week is finished when the code is in the repo, with tests, plus a few lines of takeaways in notes/weekNN.md on what you learned and what surprised you.
- **Weeks are flexible:** if a project runs long, let it. Cut learning material before cutting coding.
- **Optional items:** build items starting with “Optional:” are useful but lower priority. Skip them if the week is running long; the core items alone fit in roughly 20 weeks.
- **Bug katas:** week 1 is built around diagnosing and fixing naive code that Claude Code generates. You can ask for more katas in any later week to practise that week’s topic.
- **Short names:** courses, books, and tools are referred to by short names in the weekly sections. Full titles and links are in the appendix.

**Setup (one evening):** a Maven multi-module repo with JDK 25 (the current LTS), JMH, jcstress, Testcontainers, jOOQ, and Docker Compose for PostgreSQL. Add async-profiler and pgbench when needed.

## Phase 1: Concurrency (weeks 1–8)

*Main course: Pogrebinsky. Main book: Java Concurrency in Practice. Second explanations: Rice, Concurrent Programming in Java, and The Well-Grounded Java Developer. Rice lecture numbers follow the Coursera course (modules 1–4); each module also has a mini-project you can do in addition to the weekly build. The module 4 mini-project (Boruvka’s minimum spanning tree) is optional. Lecture titles in modules 2 and 3 may differ slightly on Coursera; the numbers are what to match. Rice is not used after Phase 1.*

### Week 1: Bug katas — diagnose and fix naive concurrent code

**Focus:** atomicity, visibility, happens-before, reordering, safe publication; learning to recognise each failure mode in plausible-looking code.

**Learn — main sources:**
- Pogrebinsky: introductory and thread-fundamentals sections.
- JCIP ch. 2, 3, and 16 (ch. 16 is the main source on the memory model).

**Learn — second explanations:**
- WGJD ch. 5 (Java concurrency fundamentals).
- Rice, module 1: 1.1 Threads.

**Build:**
- Ask Claude Code to generate 6–8 bug katas in week01-katas: small, plausible, non-thread-safe classes, each with a stress-test harness that fails some of the time. Cover lost updates, check-then-act, a worker stopped by a non-volatile flag, unsafe lazy initialisation, unsafe publication, a shared non-thread-safe collection, and a compound action on a thread-safe collection. Claude Code does not reveal which failure each kata has.
- For each kata: run the test and watch it fail; before changing any code, write the diagnosis in a DIAGNOSIS.md in the kata’s package (which failure mode, which happens-before edge is missing or which sequence is not atomic); then fix it and re-run many times.
- Plain stress tests first: an executor, a CountDownLatch start gate so threads really overlap, many iterations, and assertions on invariants.
- Introduce jcstress only for the katas a plain test cannot show reliably (visibility and reordering): Termination mode for the stop flag, and an actor/arbiter test for unsafe publication or lazy initialisation. On x86 some reorderings may never appear; record in the notes why the code is still broken.
- Ask Claude Code to review both the diagnoses and the fixes.
- Write the week’s takeaways in notes/week01.md: patterns you noticed, surprises, and any diagnoses the review showed were wrong.

### Week 2: Locks, atomics, and your own primitives

**Focus:** intrinsic locks, wait/notify, ReentrantLock, conditions, CAS, contention.

**Learn — main sources:**
- Pogrebinsky: data sharing, locking, and atomic operations sections.
- JCIP ch. 5, 13, 14, and 15.

**Learn — second explanations:**
- WGJD ch. 5 and 6 (concurrency fundamentals; atomics, locks, concurrent collections).
- Rice, module 1: 1.2 Structured Locks, 1.3 Unstructured Locks, and the Locking and Synchronization demonstration.
- Rice, module 2: 2.1 Critical Sections, 2.4 Atomic Variables, 2.5 Read-Write Isolation.
- Rice, module 3: 3.5 Bounded Buffer Problem (pairs with the bounded queue exercise).

**Build:**
- A bounded blocking queue, first with wait/notify, then with ReentrantLock and two Conditions.
- JMH benchmark: synchronized counter vs AtomicLong vs LongAdder across thread counts.
- JMH benchmark showing false sharing between two counters, fixed with padding (or @Contended).

### Week 3: Executors and liveness

**Focus:** thread pool internals, sizing, rejection, shutdown, deadlock.

**Learn — main sources:**
- JCIP ch. 6–8 and 10.
- Pogrebinsky: sections on thread pools and liveness, if covered.

**Learn — second explanations:**
- WGJD ch. 6 (executors) and ch. 5 (deadlocks).
- Rice, module 1: 1.4 Liveness, 1.5 Dining Philosophers.
- Rice, module 2: 2.2 Object-Based Isolation (monitors). (Rice does not cover executors.)

**Build:**
- A minimal thread pool from scratch (workers, task queue, shutdown()/shutdownNow(), rejection policy); compare with ThreadPoolExecutor.
- Create a deadlock with two locks in opposite order, find it in a jstack dump, fix it with lock ordering.

### Weeks 4–5: CMU buffer pool manager, in Java

**Focus:** a real concurrent component: shared state, fine-grained latching, eviction.

**Learn — main sources:**
- CMU 15-445: lectures on database storage and buffer pools.
- The CMU buffer pool project specification (use it as the design spec).

**Learn — second explanations:**
- Rice, module 2: 2.3 the concurrent spanning tree example (fine-grained, per-object locking).
- Rice, module 4: 4.1 Optimistic Concurrency, 4.4 Concurrent Hash Map (relevant to the page table).

**Build:**
- Disk manager over FileChannel with fixed 4 KB pages held in ByteBuffers.
- LRU-K replacer, page table, pin counts, dirty-page flushing.
- Thread safety throughout: decide what each lock protects; avoid deadlock between the page table and per-page latches.
- Concurrent stress tests with many threads fetching, pinning, and evicting pages.

### Week 6: Pipelines and asynchronous composition

**Focus:** bounded queues, backpressure, ordering, shutdown, CompletableFuture; keeping memory flat when the input is huge.

**Learn — main sources:**
- Pogrebinsky: sections on inter-thread communication.
- pgjdbc documentation: getting results based on a cursor (fetch size), and the CopyManager API.
- GC logging basics: -Xlog:gc\* and reading pause times.

**Learn — second explanations:**
- WGJD ch. 6 (CompletableFuture), ch. 7 (garbage collection basics), ch. 16 (advanced concurrent programming).
- Rice, module 3: 3.1 Actors, 3.2 Actor Examples, 3.3 Sieve of Eratosthenes, 3.4 Producer-Consumer Problem (actors are a useful contrast to a queue-based pipeline).
- Rice, module 4: 4.2 Concurrent Queue, 4.3 Linearizability.

**Build:**
- Producer–consumer pipeline over a large generated file: one reader, N workers, one writer, bounded queues, sequence numbers with a reorder buffer, poison-pill shutdown, per-record error handling.
- A second source: tens of millions of rows from PostgreSQL. Start naive (read the whole result set) with a small heap (e.g. -Xmx512m) and watch GC logs: long stop-the-world pauses, then OutOfMemoryError. Note that pgjdbc fetches the entire result into memory by default.
- Fix it with cursor-based fetching: setFetchSize with autocommit off (pgjdbc ignores the fetch size in autocommit mode), feeding the bounded queue so memory stays flat.
- Alternative: COPY ... TO STDOUT through pgjdbc’s CopyManager, streamed into the pipeline. Compare throughput and memory with the cursor approach.
- A CompletableFuture aggregator calling simulated slow services with timeouts and fallbacks.
- Profile the pipeline with async-profiler and fix its top bottleneck.

### Week 7: Virtual threads — where they help and where they hurt

**Focus:** I/O-bound vs CPU-bound work, limited downstream resources, Amdahl’s law.

**Learn — main sources:**
- Pogrebinsky: virtual threads and performance sections.
- JEP 444 (virtual threads) and JEP 491 (why synchronized no longer pins since Java 24).
- JCIP ch. 11 and 12.

**Learn — second explanations:**
- Rice has nothing on virtual threads (the course is Java 8-era); Amdahl’s law is covered in module 1 of the first course in the series, Parallel Programming in Java.
- Optional: Kabutz, Mastering Virtual Threads in Java, if the topic still feels shaky.

**Build:**
- Load test with 10,000 concurrent blocking calls: platform thread pool vs virtual threads.
- The connection-pool trap: 10,000 virtual threads hitting PostgreSQL through a HikariCP pool of 10. Observe the waiting and timeouts, then limit concurrency with a Semaphore.
- CPU-bound tasks on virtual threads vs a platform pool sized to the cores; show there is no gain and observe carrier starvation.
- Optional, 30 minutes: install JDK 21 with SDKMAN and watch the jdk.VirtualThreadPinned JFR event fire for blocking inside synchronized, to understand the issue still found in Java 21 codebases.

### Week 8: Scoped values, structured concurrency, and observing virtual threads

**Focus:** ScopedValue (JEP 506, final in 25), StructuredTaskScope (JEP 505, preview in 25), JFR and JDK Mission Control.

**Learn — main sources:**
- JEP 506 and JEP 505, read in full: motivation, API, and the comparison with ThreadLocal and CompletableFuture.
- JDK Mission Control documentation; the JFR event list for virtual threads.

**Learn — second explanations:**
- Optional: Kabutz, Mastering Virtual Threads in Java (covers both APIs and diagnosis).

**Build:**
- ScopedValue: propagate a request context (request id, tenant, deadline) through a deep call chain without passing parameters. Rebind it in a nested scope, and show that forked StructuredTaskScope subtasks inherit it.
- Compare with ThreadLocal and InheritableThreadLocal: mutability, leaks when a value is never removed, and memory with a million virtual threads.
- StructuredTaskScope (--enable-preview): fan out with the all-successful joiner and show that one failure cancels the siblings; hedged requests with the any-successful joiner; timeouts through the scope configuration. The API is still in preview, so focus on the concepts rather than the details.
- Optional: a custom Joiner that collects partial results.
- Rewrite the week-6 CompletableFuture aggregator with StructuredTaskScope and compare: what happens to running subtasks when the caller is interrupted or times out in each version.
- Record with JFR, enabling the virtual-thread events that are off by default (jdk.VirtualThreadStart and End), plus jdk.VirtualThreadPinned and jdk.VirtualThreadSubmitFailed. Analyse the recording in JDK Mission Control.
- Take a JSON thread dump with jcmd \<pid\> Thread.dump_to_file -format=json and find the structured task scope tree in it.
- Optional: use the JFR streaming API (RecordingStream) to log pinning and long GC pauses from inside the running application.

## Phase 2: Databases (weeks 9–17)

*Main course: Nasser. Main book: PostgreSQL 14 Internals, plus the PostgreSQL, PgBouncer, and pgjdbc documentation. Second explanations: DDIA ch. 7–8, Postgres Professional materials, CMU lectures.*

### Week 9: Big data, query plans, indexes

**Focus:** EXPLAIN (ANALYZE, BUFFERS), the planner, B-tree, composite/covering/partial indexes, bulk loading.

**Learn — main sources:**
- Nasser: indexing sections.
- Rogov Part IV (ch. 16–20) and ch. 25.

**Learn — second explanations:**
- Postgres Professional: query performance course materials.
- CMU 15-445: lectures on indexes and query execution.

**Build:**
- Generate 100 million transactions with generate_series; measure typical queries before and after indexing.
- Bulk-load three ways (row by row, jOOQ batch, COPY) and compare throughput.

### Weeks 10–12: CMU B+ tree index, in Java

**Focus:** how an index really works, and concurrency inside a data structure.

**Learn — main sources:**
- CMU 15-445: lectures on tree indexes and index concurrency control.
- The CMU B+ tree project specification.

**Learn — second explanations:**
- Rogov ch. 25 (B-tree), for how PostgreSQL does it.

**Build:**
- B+ tree stored in pages from your week 4–5 buffer pool.
- Insert with node splits, delete with merges and redistribution, range iterator.
- Latch crabbing: release a parent’s latch once the child is known to be safe.
- Concurrent correctness tests, and a benchmark against ConcurrentSkipListMap.

### Week 13: Transactions and isolation

**Focus:** MVCC, isolation levels, anomalies, row locks, deadlocks, optimistic locking.

**Learn — main sources:**
- Nasser: ACID and concurrency control sections.
- Rogov Part I (ch. 2–7) and Part III (ch. 12–13).

**Learn — second explanations:**
- DDIA ch. 8 (Transactions).
- CMU 15-445: concurrency control and MVCC lectures.

**Build:**
- Transfer service with jOOQ, driven by concurrent transfers from a Java executor.
- Show lost updates, then fix with FOR UPDATE, version columns, and a conditional update.
- Trigger and fix a deadlock between opposite-direction transfers.
- Reproduce write skew under Repeatable Read; handle it with Serializable plus retries.

### Week 14: Partitioning and job queues

**Focus:** range and hash partitioning, pruning, data lifecycle, SKIP LOCKED, outbox.

**Learn — main sources:**
- Nasser: partitioning sections.
- PostgreSQL documentation: table partitioning, and SELECT ... FOR UPDATE SKIP LOCKED.

**Build:**
- Convert the 100M-row table to monthly range partitions and measure pruning; detach and archive an old partition.
- Job queue using SKIP LOCKED with competing workers.
- Outbox table with a relay that publishes events exactly once.

### Week 15: Online schema changes and maintenance

**Focus:** changing a large, busy table without locking it for hours: DDL lock levels, lock queues, table rewrites, CREATE INDEX CONCURRENTLY, pg_repack, expand–contract migrations.

**Learn — main sources:**
- PostgreSQL documentation: explicit locking (table-level lock modes), ALTER TABLE, CREATE INDEX (the “Building Indexes Concurrently” section).
- Rogov ch. 12 (relation-level locks), revisited with DDL in mind.

**Learn — second explanations:**
- pg_repack documentation (for the optional item).

**Build:**
- Keep a load generator writing to the 100M-row table throughout the week.
- Run a series of ALTER TABLE operations under load and classify each: instant (e.g. adding a nullable column, or one with a constant default), blocking but quick, or rewriting the whole table (e.g. changing a column type).
- Reproduce the lock-queue pile-up: a long-running transaction holds a weak lock, an ALTER waits for ACCESS EXCLUSIVE, and every new query queues behind it. Fix it with lock_timeout and a retry loop.
- Add NOT NULL and foreign-key constraints safely: add them as NOT VALID, then VALIDATE CONSTRAINT separately.
- CREATE INDEX vs CREATE INDEX CONCURRENTLY under write load; make a concurrent build fail, then find and clean up the INVALID index it leaves behind.
- Optional: create heavy bloat with mass updates; compare VACUUM FULL (exclusive lock for the whole run) with pg_repack (online).
- Optional: change a column’s type with expand–contract: add the new column, dual-write, backfill in batches, switch reads, drop the old column.

### Week 16: Replication and connection pooling

**Focus:** streaming replication, lag, read-your-writes, HikariCP sizing, PgBouncer pool modes and their pitfalls.

**Learn — main sources:**
- Nasser: replication sections.
- PgBouncer documentation: pool modes, the feature table for transaction mode, prepared statement support.
- pgjdbc documentation: server-side prepared statements and prepareThreshold.

**Learn — second explanations:**
- Rogov Part II (skim).
- Postgres Professional: replication-related materials.

**Build:**
- Primary plus streaming replica in Docker Compose; route reads to the replica and observe lag breaking read-your-writes.
- Find the right HikariCP pool size with pgbench and your own load test.
- PgBouncer session vs transaction mode with thousands of virtual-thread clients: compare server connection counts and throughput, and see why session mode gives no multiplexing.
- Transaction-mode pitfalls: session state (SET, advisory locks, LISTEN, temporary tables) silently leaking between clients.
- Prepared statements: run jOOQ through PgBouncer in transaction mode until pgjdbc switches to server-side prepared statements (after prepareThreshold executions) and reproduce the errors. Fix it two ways, with PgBouncer’s protocol-level prepared statement support (max_prepared_statements) and with prepareThreshold=0, and measure the difference.

### Week 17: Sharding

**Focus:** shard keys, consistent hashing, cross-shard queries, global IDs, connection budgets across shards.

**Learn — main sources:**
- Nasser: sharding sections.

**Learn — second explanations:**
- DDIA ch. 7 (Sharding).

**Build:**
- Citus in Docker: distribute by account_id; compare co-located and cross-shard queries.
- Application-level router with consistent hashing over several PostgreSQL instances.
- Thread-safe Snowflake-style ID generator.
- Connection budget: tens of thousands of virtual threads across 10 shards. Per-shard Semaphore, per-shard HikariCP pool, PgBouncer per shard; work out the total server connections against max_connections.
- Slow down one shard and show head-of-line blocking without per-shard limits, then isolate it with per-shard bulkheads and timeouts.

## Phase 3: Distributed systems and design (weeks 18–21)

*Main book: Designing Data-Intensive Applications (2nd ed.). Second explanation: selected MIT 6.5840 lectures.*

### Weeks 18–19: Distributed transactions and consistency

**Focus:** atomic commit across databases, failures in the middle of a commit, distributed deadlocks, sagas, idempotency, consistency models.

**Learn — main sources:**
- DDIA ch. 6 (Replication), 9 (partial failures), and 10 (consistency and consensus), including the two-phase commit material.
- PostgreSQL documentation: PREPARE TRANSACTION, COMMIT PREPARED, and pg_prepared_xacts.

**Learn — second explanations:**
- MIT 6.5840: lectures on Raft, linearizability, and Spanner.

**Build:**
- Transfer between two shards with two-phase commit: PREPARE TRANSACTION on both (max_prepared_transactions \> 0), then COMMIT PREPARED, driven by a Java coordinator with a durable decision log.
- Failure injection with Toxiproxy or a Docker network disconnect: lose the network, or kill the coordinator, after the first COMMIT PREPARED and before the second. Observe the in-doubt transaction in pg_prepared_xacts still holding its locks, and write the recovery procedure that resolves it from the decision log.
- The same transfer as a saga with compensation; compare guarantees, latency, and failure behaviour with two-phase commit.
- Distributed deadlock: work out (in the notes, not code) why two opposite-direction transfers across shards, each holding a row lock on its own shard and waiting for the other, deadlock without either PostgreSQL instance detecting it, and how global lock ordering, lock_timeout, and timeouts with retries prevent it.
- Optional: reproduce that distributed deadlock in code and fix it; compare with Citus’s distributed deadlock detection.
- Idempotency keys end to end; failure injection to prove there is no double-spending.

### Weeks 20–21: Capstone

**Focus:** bringing everything together, including choosing a garbage collector for large heaps and low latency.

**Learn — main sources:**
- JEP 439 (Generational ZGC) and the HotSpot GC tuning guide sections on G1 and ZGC.
- Practice system design out loud: ledger, wallet, rate limiter, file-ingestion pipeline.

**Learn — second explanations:**
- WGJD ch. 7 (Understanding Java performance: measurement, GC, JIT).

**Build:**
- One service: high-throughput ingestion (Phase 1 pipeline) into partitioned PostgreSQL, outbox, correct concurrent balance updates, load test.
- GC comparison under that load with as large a heap as your machine allows: G1 vs ZGC (-XX:+UseZGC). Measure p99 and p99.9 latency with HdrHistogram, pause times from GC logs and JFR, and CPU cost. A rough comparison is enough; the point is the trade-off: ZGC keeps pauses short regardless of heap size, in exchange for more CPU and some throughput.
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
- **Kabutz (optional, weeks 7–8):** *Mastering Virtual Threads in Java* — Dr Heinz M. Kabutz, JavaSpecialists; virtual threads, structured concurrency, scoped values, diagnosing deadlocks. Buy only if virtual threads still feel shaky by weeks 7–8. Listed under the Concurrency category. <https://javaspecialists.teachable.com/courses/category/Concurrency>
- **Kabutz (optional, later):** *Extreme Java – Concurrency Performance* — Dr Heinz M. Kabutz, JavaSpecialists; self-paced, about 14.5 hours, a modernised companion to Java Concurrency in Practice. Only if gaps remain after Phase 1. <https://javaspecialists.eu/courses/jpt/>
- **Kabutz (free):** *The Java Specialists’ Newsletter* — Dr Heinz M. Kabutz; long-running free archive with many concurrency articles. <https://www.javaspecialists.eu/archive/>
- **Nasser:** *Fundamentals of Database Engineering* — Hussein Nasser, Udemy. <https://www.udemy.com/course/database-engines-crash-course/>
- **Postgres Professional:** *PostgreSQL training courses (e.g. query performance tuning, administration)* — Postgres Professional; free slides and videos. <https://postgrespro.com/education/courses>
- **CMU 15-445:** *CMU 15-445/645: Intro to Database Systems* — Carnegie Mellon University (Andy Pavlo); lectures, notes, and project specifications. Lecture videos on the CMU Database Group YouTube channel. <https://15445.courses.cs.cmu.edu/>
- **MIT 6.5840:** *MIT 6.5840: Distributed Systems (formerly 6.824)* — MIT (Robert Morris); schedule, papers, lecture videos, and labs. <https://pdos.csail.mit.edu/6.824/>

### Books

- **JCIP:** *Java Concurrency in Practice* — Brian Goetz, Tim Peierls, Joshua Bloch, Joseph Bowbeer, David Holmes, Doug Lea; Addison-Wesley, 2006. <https://jcip.net/>
- **DDIA:** *Designing Data-Intensive Applications, 2nd edition* — Martin Kleppmann and Chris Riccomini; O’Reilly, February 2026. Chapter numbers in this plan follow the second edition. <https://dataintensive.net/>
- **WGJD:** *The Well-Grounded Java Developer, 2nd edition* — Benjamin J. Evans, Jason Clark, Martijn Verburg; Manning, 2022. Most relevant: ch. 5 (concurrency fundamentals), 6 (JDK concurrency libraries), 7 (performance and GC), 16 (advanced concurrent programming); ch. 14 covers Testcontainers. Its virtual-threads material predates their final release, so prefer the JEPs for weeks 7–8. <https://www.manning.com/books/the-well-grounded-java-developer-second-edition>
- **Rogov:** *PostgreSQL 14 Internals* — Egor Rogov, Postgres Professional; free PDF. <https://postgrespro.com/community/books/internals>

### Tools and references

- **JMH:** *Java Microbenchmark Harness* — OpenJDK. <https://github.com/openjdk/jmh>
- **jcstress:** *Java Concurrency Stress tests* — OpenJDK. <https://github.com/openjdk/jcstress>
- **async-profiler:** *async-profiler* — sampling profiler for Java. <https://github.com/async-profiler/async-profiler>
- **JEP 444:** *JEP 444: Virtual Threads* — OpenJDK, final in Java 21. <https://openjdk.org/jeps/444>
- **JEP 491:** *JEP 491: Synchronize Virtual Threads without Pinning* — OpenJDK, Java 24. <https://openjdk.org/jeps/491>
- **JEP 505:** *JEP 505: Structured Concurrency (Fifth Preview)* — OpenJDK, Java 25. <https://openjdk.org/jeps/505>
- **JEP 506:** *JEP 506: Scoped Values* — OpenJDK, final in Java 25. <https://openjdk.org/jeps/506>
- **JEP 439:** *JEP 439: Generational ZGC* — OpenJDK, Java 21; generational is the only ZGC mode since Java 24. <https://openjdk.org/jeps/439>
- **JMC:** *JDK Mission Control* — tool for analysing JFR recordings. <https://jdk.java.net/jmc/>
- **HdrHistogram:** *HdrHistogram* — latency percentile recording. <https://github.com/HdrHistogram/HdrHistogram>
- **pgjdbc:** *PostgreSQL JDBC Driver documentation* — fetch size and cursors, CopyManager, prepareThreshold. <https://jdbc.postgresql.org/documentation/>
- **PostgreSQL locking:** *PostgreSQL documentation: Explicit Locking* — table-level lock modes and conflicts. <https://www.postgresql.org/docs/current/explicit-locking.html>
- **PREPARE TRANSACTION:** *PostgreSQL documentation: PREPARE TRANSACTION* — two-phase commit in PostgreSQL. <https://www.postgresql.org/docs/current/sql-prepare-transaction.html>
- **pg_repack:** *pg_repack* — online table and index reorganisation. <https://reorg.github.io/pg_repack/>
- **Toxiproxy:** *Toxiproxy* — network failure simulation. <https://github.com/Shopify/toxiproxy>
- **SDKMAN:** *SDKMAN!* — for installing and switching between JDK versions. <https://sdkman.io/>
- **Testcontainers:** *Testcontainers for Java*. <https://java.testcontainers.org/>
- **jOOQ:** *jOOQ* — typesafe SQL for Java. <https://www.jooq.org/>
- **HikariCP:** *HikariCP* — JDBC connection pool; see its wiki page “About Pool Sizing”. <https://github.com/brettwooldridge/HikariCP>
- **PgBouncer:** *PgBouncer* — lightweight PostgreSQL connection pooler. <https://www.pgbouncer.org/>
- **pgbench:** *pgbench* — PostgreSQL benchmarking tool (PostgreSQL docs). <https://www.postgresql.org/docs/current/pgbench.html>
- **Citus:** *Citus* — distributed PostgreSQL extension. <https://github.com/citusdata/citus>
