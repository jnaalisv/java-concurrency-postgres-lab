# LatencyStats: diagnosis

## Symptom
We don't see expected number of updates.

## Failure mode
Lost update

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `views` (long) | `record()` | `total()` | not protected properly |

## Mechanism
`views++` is three separate actions: read, add one, write. Two threads can both read 0 and both write 1, so one 
increment disappears. Volatile only provides visibility guarantee, not atomicity

## Why it is intermittent
It's not

## What the fix must guarantee
That writes to `views` are atomic.

## Fix and verification
make `record()` synchronized.