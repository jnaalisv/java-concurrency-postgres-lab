# rates: diagnosis

## Symptom
Two withdrawals can both read a sufficient balance, both pass the check, and both subtract. The
wallet then goes negative, breaking the invariant the class exists to protect

## Failure mode
check-then-act

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `balanceCents` (AtomicLong) |  constructor  | `deposit()`,`withdraw()`,`balanceCents()` | The type itself is thread safe |

## Mechanism
`get()` and `addAndGet()` are each atomic, but the sequence of the two isn’t.

## Why it is intermittent
depends on the interleaving; a passing run proves nothing

## What the fix must guarantee
any sequence combining `get()` and `addAndGet()` must be atomic.

## Fix and verification
We use `compareAndSet` to make the update atomic. Alternatively, all methods accessing `balanceCents` could be made 
synchronized and the type could then be just `long`.

Unit test passes, jcstress test passes.
