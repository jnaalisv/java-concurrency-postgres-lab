# wallet: diagnosis

## Symptom
negative balance

## Failure mode
check-then-act

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `balanceCents` (AtomicLong) |  `deposit()`,`withdraw()`  | `deposit()`,`withdraw()`,`balanceCents()` | `withdraw()` is not thread safe |

## Mechanism
`get()` and `addAndGet()` are each atomic, but the sequence of the two isn’t, so two withdrawals can both read a 
sufficient balance, both pass the check, and both subtract. The wallet then goes negative, breaking the invariant the 
class exists to protect. 

For example, Threads A and B intent to withdraw 60 while balance is 100. Both threads read balance 100 and proceed to 
update `balanceCents` with -60, resulting in a balance of -20.

## Why it is intermittent
depends on the interleaving; a passing run proves nothing

## What the fix must guarantee
any sequence combining `get()` and `addAndGet()` must be atomic.

## Fix and verification
We use `compareAndSet` to make the update atomic. Alternatively, all methods accessing `balanceCents` could be made 
synchronized and the type could then be just `long`.

Unit test passes, jcstress test passes.

# After review
### Questions

1. A single `withdraw()` can retry forever if another thread constantly interleaves it and changes the balance so that 
   the balance is high enough for withdrawal. With CAS loop we will retry as long as balance allows for withdrawal. A
   synchronized `withdraw()` would return immediately.

2. It's not a bug, the result is that the withdrawal got delayed or re-ordered.
3. Via boolean array (or AtomicBoolean).
