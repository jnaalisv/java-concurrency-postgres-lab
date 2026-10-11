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

## 2026-10-11, Final word

This responds to the "After review" section and the revised diagnosis in 93a9c99. The code is
unchanged since ffe87d6, so the earlier test results still stand.

**Verdict.** Diagnosis: correct. Fix: correct. Unchanged.

### Your answers

**Finding 1, the map's contents: row added, but the question it raised is only answered
indirectly.** Your answer to question 1 contains the explanation: final-field semantics cover
what was written *by the end of the constructor*. In the original code, the `put()` calls came
after the constructor had returned, so the freeze covered an empty map. Write that sentence
under the mechanism, where it belongs.

**Finding 2, the three changes: mostly right.**

- Your reason for `volatile` is a good one. It makes the publication safe however `RateTable`
  is written, instead of relying on another class staying immutable.
- Change 2 has no reason given. It's the fix for the second racy read that could return
  null, which you found yourself.
- The results are still not recorded. "Jcstress tests must pass" is a requirement, not a
  result. Every kata so far has had this gap, so it belongs in your week takeaways.

**Finding 3: fixed.**

**Question 1: right, and well argued.** It is initialization safety: a final field set in the
constructor, `this` not escaping, and everything reachable through the field as of the
constructor's end. One precondition is worth saying out loud. This only holds together with
change 2. With two racy reads of a non-volatile `table`, the second could still return null,
however immutable `RateTable` is.

**Question 2: one alternative, and its cost overstated.** A `synchronized` `table()` is
correct. Its cost is a lock acquisition on every call, which is cheap uncontended and only
expensive under contention. The question asked for simpler designs. The other obvious one is
to fetch eagerly in the constructor and keep the table in a `final` field. That gives no
laziness and no locking at all. The price is that every converter fetches even if it's never
used, and that construction becomes slow and can fail. Whether that's acceptable depends on
how converters are created, and that is the design question.

**Question 3: right about the behaviour, but you didn't say whether it's a problem.** Other
callers block for the whole fetch. On an exception the lock is released, `table` stays null,
and the next caller fetches again. Here, blocking is mostly what you want, because callers need
the rates anyway. The risks of holding a lock across alien code are that a hung fetch blocks
every caller with no timeout, and that a fetch which calls back into the converter from another
thread deadlocks.

### What to study again

- **JCIP 16.2.3 and 16.2.4**, safe initialization idioms and double-checked locking. Gap:
  question 2, the alternatives to double-checked locking.
- **JCIP 10.1.4, Open calls** (week 3 reading). Gap: question 3, holding a lock across alien
  code.

### Follow-up exercise

Optional: check your question 1 argument. Remove `volatile` from `table`, keep the other two
changes, and run `CurrencyConverterLazyInitTest`. Then remove change 2 as well and run it
again.

**This kata is closed.**
