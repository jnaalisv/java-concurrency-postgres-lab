# rates: diagnosis

## Symptom
Saw table reference before its contents, jcstress outcome `3`

## Failure mode
unsafe lazy initialization, via broken double-checked locking, where the rate table is unsafely published

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `CurrencyConverter.table` (RateTable) |  `toEur()`  | `toEur()` | not protected |
| `RateTable.ratesToEur` (Map<String, BigDecimal>) |  `put()`  | `rateToEur()` | not protected |

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
Safe publication of `CurrencyConverter.table` and that rates are fetched exactly once.

## Fix and verification
1. `table` is now volatile, communicate intent and the correctness doesnt depend on RateTable implementation
2. the fast path reads `table` once, into a local variable;
3. `RateTable` is immutable, built from `Map.copyOf` in its constructor, which guarantees safe initialization

Jcstress tests must pass.

# After review

1. Strictly, no. With the immutable RateTable, the code is correct without volatile. 

RateTable is safely initialized. Its only field is final, it’s set in the constructor, and this doesn’t escape the 
constructor. The JMM then guarantees that any thread seeing a RateTable reference also sees ratesToEur, plus everything reachable through
it as of the end of the constructor: the Map.copyOf result’s internal array and the BigDecimals.

2. synchronizing the method `table()`, but that would be slower.

3. Other callers will block, if the method throws, the lock will be released and another call can enter the synchronized 
   block.
