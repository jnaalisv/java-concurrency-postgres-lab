# rates: diagnosis

## Symptom
Saw table reference before its contents

## Failure mode
unsafe lazy initialization, via broken double-checked locking, where the rate table is unsafely published

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `CurrencyConverter.table` (RateTable) |  `toEur()`  | `toEur()` | not protected |

## Mechanism
`CurrencyConverter.table`  isn’t volatile, and the fast path reads it without taking the lock. Take two threads, A and B:

Thread A takes the lock. It builds a RateTable, fills its HashMap with the rates, then assigns table = loaded.
Thread B reads table without the lock and sees it non-null. Nothing orders A’s writes before that read, so B can see the 
reference to the RateTable before A’s put() calls into the HashMap are visible.

There’s also a smaller bug. The fast path reads table twice: once in the if, once in the return. Those two plain
reads are unordered, so in theory the second one can return null after the first one saw a non-null value.

The current synchronized block creates a happens-before edge only between threads that both 
take it, and B on the fast path never does.

## Why it is intermittent
depends on the interleaving; a passing run proves nothing

## What the fix must guarantee
Safe publication of `CurrencyConverter.table`.

## Fix and verification
Make `CurrencyConverter.table` and init it via constructor. Jcstress tests must pass.