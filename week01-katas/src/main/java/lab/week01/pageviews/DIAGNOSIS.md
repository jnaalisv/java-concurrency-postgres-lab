# PageViewCounter: diagnosis

## Symptom
We don't see expected number of updates.

## Failure mode
Lost update

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `views` (long) | `record()` | `total()`, `record()` | `volatile`, which gives visibility only |

## Mechanism
`views++` is three separate actions: read, add one, write. Two threads can both read 0 and both write 1, so one 
increment disappears. Volatile only provides visibility guarantee, not atomicity

## Why it is intermittent
It is (this was a silly mistake)

## What the fix must guarantee
That writes to `views` are atomic.

## Fix and verification
make `record()` synchronized, without it both jcstress and PageViewCounterStressTest don't pass.

# After review
`views++` is three actions: read, add one, write, these three actions need to be atomic so that the writes are visible 
to readers.

The edge is the `Future` handoff: actions taken by the task happen-before `Future.get()` returns in another thread

### Questions

1. `volatile` is not needed anymore. On a 32bit JVM, reads might tear. I believe I cant reproduce that on my machine.

2. Under high contention `LongAdder` performs better than `AtomicLong.incrementAndGet()`, and the latter performs 
better than `synchronized` methods because `AtomicLong` is implemented via native CAS, and it does not need to block 
threads.

3. Perhaps it was easier to prove the bug with jcstress test in this case.