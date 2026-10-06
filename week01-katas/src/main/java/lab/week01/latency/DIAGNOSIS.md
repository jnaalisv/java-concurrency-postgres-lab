# LatencyStats: diagnosis

## Symptom
The latencies are constant 250, so the mean observed by monitor threads should always be either 0 or exactly 250. But 
this is not the case.

## Failure mode
Inconsistent lock scope, writes are guarded but reads are not. 

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `count` (long) | recorder threads via `record()` | main thread via `count()`, monitor threas via `meanMicros()` | writes are syncronized, reads are not guarded |
| `totalMicros` (long) | recorder threads via `stop()` |  monitor threas via `meanMicros()` | writes are syncronized, reads are not guarded |
| `maxMicros` (long) | recorder threads via `stop()` |  not read | writes are syncronized |

## Mechanism

## Why it is intermittent
Its not.

## What the fix must guarantee
Writes to `count` and `totalMicros` must synchronizes-with every later read of `count` and `totalMicros`

## Fix and verification
Make `meanMicros()` synchronized.
