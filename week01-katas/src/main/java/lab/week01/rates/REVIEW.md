# CurrencyConverter: review

## 2026-10-11, reviewing ffe87d6

**Verdict.** Diagnosis: correct. Fix: correct.

This was the hardest kata in the set, and your diagnosis is the best yet. You found the broken
double-checked locking, the unsafely published HashMap contents, and the second racy read that
can return null. That last one is subtle, and most people miss it. You also wrote a contract
jcstress test that catches the bug. What's left:

1. The shared-state table leaves out the state that actually goes wrong (finding 1).
2. Say which of your three changes fixes which problem, and record the results (finding 2).

**Test results.**

- `CurrencyConverterStressTest` at ffe87d6: passed 5 out of 5 runs. It also passed on the
  original code in every run I made when the katas were generated, which is expected for this
  kind of bug.
- `CurrencyConverterLazyInitTest`, default mode, run in scratch copies outside the repo:
  - At ffe87d6: about 655 million samples, all `1, 1, 1`. Passed.
  - On the original code (4ae52d8): `1, 3, 1` 42 times and `3, 1, 1` 35 times, in about 533
    million samples. Failed, as it should: a caller saw the table but not its rates. The `4`
    outcome, the null from the second read, didn't appear. That doesn't make it impossible.

### Findings

**1. The shared-state table leaves out the map's contents. (Medium)** Your mechanism turns on
the rates written into `RateTable`'s HashMap after the table object was built, but the table
lists only `CurrencyConverter.table`. Add a row for the map's contents. Then answer the
question your row raises. In the original code, `ratesToEur` was already a `final` field, so
why didn't final-field semantics protect its contents? Look at *when* the `put()` calls
happened relative to the end of the constructor (JLS 17.5, JCIP 16.3).

**2. "Fix and verification" is garbled, and the results aren't recorded. (Medium)**

- "Make `CurrencyConverter.table` and init it via constructor" is missing a word, and it mixes
  two changes in two classes.
- You made three changes:
  - `table` is now volatile;
  - the fast path reads `table` once, into a local variable;
  - `RateTable` is immutable, built from `Map.copyOf` in its constructor.
- Say which problem from your mechanism each one fixes, and record what both tests showed on
  the original code and on the fix.

**3. Minor points. (Minor)**

- "Symptom" should say where you saw it: jcstress outcome `3`, which is rare. The plain stress
  test never showed it.
- "What the fix must guarantee" names safe publication but leaves out the README's other
  requirement, that rates are fetched exactly once. Your fix keeps it: the second check happens
  under the lock. Say so.

### Questions

Answer these in an "After review" section of `DIAGNOSIS.md`, together with your response to
the findings.

1. Are all three changes necessary? Suppose you keep the immutable `RateTable` and the single
   read into a local variable, but leave `table` non-volatile. Would that be correct? Argue
   from JLS 17.5, then check with your jcstress test.
2. Double-checked locking is the most fragile way to initialise lazily. Which simpler designs
   would avoid it here, and what would each cost, given that fetching is slow?
3. `fetchRatesToEur()` runs while the converter's lock is held. What happens to other callers
   while it runs, and what happens if it throws? The concurrency checklist has a name for
   calling unknown code under a lock. Is it a problem here?
