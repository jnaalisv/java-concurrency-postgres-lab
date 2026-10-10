# RequestQuota: diagnosis

## Symptom
RequestQuota grants more requests than its limit

## Failure mode
check-then-act, lost update

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `used` (ConcurrentHashMap) | `tryAcquire()`,`reset()` writes through `clear()`| `tryAcquire()`, `used()` | *sequence* of calls inside `tryAcquire()` are not protected |

## Mechanism
T1 and T2 both simultaneously call `tryAcquire()` with same id C1. Then T1 calls `used.getOrDefault` and gets a value, 
but before T1 can call `used.put`, T2 calls `used.getOrDefault` and gets the same value. Now both threads will call
`used.put` with `current + 1` and return true, so 2 requests for the same client will be allowed while we lose one 
counter increment. This can happen with more than 2 threads interleaving, so we can potentially lose more counter 
increments.

`reset()` can also interleave with `tryAcquire()`: T1 calls `tryAcquire()` gets the current value from `used`, T2 calls
`reset()`, T1 continues executing and update  `used` with  `current + 1` but now `current` is the value read just before
reset.

## Why it is intermittent
Intermittent, depends on how the calling threads interleave. Less calling threads and you we might not see it.

## What the fix must guarantee
That the read and modify inside `tryAcquire()` are atomic.

## Fix and verification
We could make `tryAcquire()` `synchronized`, but I have chosen a different approach, to use `ConcurrentMap::compute` 

The stress test covers concurrent acquires; the jcstress test covers reset against acquire.

# After review

### Questions

1. Yes its safe with `ConcurrentHashMap`? The field is declared as `ConcurrentMap`. It would not be safe for 
    `ConcurrentSkipListMap`, its javadoc says: "The function is NOT guaranteed to be applied once atomically."

2. A `synchronized` `tryAcquire()` would serialise on the RequestQuota, while my version using compute
   would only block on "some" other threads updating the map. So in the given scenario my version would block less.

3. The `reset()` calls are not atomic across clients. The README's contract doesn't require it
