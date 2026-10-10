# RequestQuota: diagnosis

## Symptom
RequestQuota grants more requests than its limit

## Failure mode
check-then-act

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `used` (ConcurrentHashMap) | `tryAcquire()`,`reset()` | `tryAcquire()`, `used()` | not protected |

## Mechanism
T1 and T2 both simultaneously call `tryAcquire()` with same id C1. Then T1 calls `used.getOrDefault` and gets a value, 
but before T1 can call ` used.put`, T2 calls `used.getOrDefault` and gets the same value.

## Why it is intermittent
Not intermittent, it fails all the time when there are more than one thread accessing RequestQuota.

## What the fix must guarantee
That the read and modify inside `tryAcquire()` are atomic.

## Fix and verification
We could make `tryAcquire()` `synchronized`, but I have chose a different approach, to use `ConcurrentMap::compute` 