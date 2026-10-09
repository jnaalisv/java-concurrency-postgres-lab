# LatencyStats: diagnosis

## Symptom
The latencies are constant 250, so the mean observed by monitor threads should always be either 0 or exactly 250. But 
this is not the case.

## Failure mode
Inconsistent lock scope, writes are guarded but reads are not. 

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `count` (long) | recorder threads via `record()` | main thread via `count()`, monitor threads via `meanMicros()` | writes are synchronized, reads are not guarded |
| `totalMicros` (long) | recorder threads via `record()` |  monitor threads via `meanMicros()` | writes are synchronized, reads are not guarded |
| `maxMicros` (long) | recorder threads via `record()` |  not read | writes are synchronized |

## Mechanism

## Why it is intermittent
Its not.

## What the fix must guarantee
Writes to `count` and `totalMicros` must synchronizes-with every later read of `count` and `totalMicros`

## Fix and verification
Make `meanMicros()` synchronized.

# After review
## Mechanism
Without synchronizing the method `totalMicros`, the monitor threads can interleave their reads with the recorder
threads:
recorder-thread: call `record`, enter the synchronized block
recorder-thread: write `count`
monitor-thread: call `meanMicros`
monitor-thread: calculate `totalMicros`(not updated) / `count` (updated) => we get wrong value
recorder-thread: write `totalMicros`
Possible interleavings for `totalMicros` / `count`:
    0 / 1 => 0, indistinguishable interleaving during first call to `record`
    50 / 2 => 25.0, interleaving during a second call to `record`
    (50 + 250) / 1 => 150.0, observed via jcstress, seems like writes to `totalMicros` and `count` where reordered
and while monitor thread interleaved with the record thread.

## What the fix must guarantee
The un-lock at the end of `record()` synchronizes-with the lock at the start of a later `meanMicros()`. Therefore, every 
write made in `record()` happens-before every read made in meanMicros()

Volatile would also provide visibility, but it won't guard against the threads interleaving. Making the method 
synchronized does guard against the interleaving, it makes the reads and write to `count` and `totalMicros` atomic.

The methods `count()` and `maxMicros()` must also be synchronized, volatile wont be enough because their updates use
their previous state, and being 64 bit values they could tear under some conditions.

Yes the interleaving is not guaranteed to happen, it can happen without proper synchronization.

### Questions

1. It's thread safe because of "thread termination rule", any action (writes to count) happens in thread before any other 
thread detects the thread has terminated. When the main thread calls `count()`, all the other threads are terminated.
2. Monitoring threads would not need to obtain monitor lock but the recorders would have to allocate a new immutable 
3. record every time.
3. No, because we would still need the atomicity provided by a monitor lock.
