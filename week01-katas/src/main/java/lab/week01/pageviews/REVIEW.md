# PageViewCounter: review

## 2026-10-09, reviewing 2dcb46e

**Verdict.** Diagnosis: correct. Fix: correct.

The failure mode and the mechanism are right, and so is the point that `volatile` gives
visibility but not atomicity. The fix and the jcstress test are both correct. What needs work
is precision in the later sections:

1. State the guarantee precisely. Atomicity of what, and visibility for whom (finding 1).
2. Your own jcstress numbers contradict "it's not intermittent" (finding 2).
3. Record how you verified the fix (finding 3).

**Test results.**

- `PageViewCounterStressTest` at 2dcb46e: passed 10 out of 10 runs.
- `PageViewCounterLostUpdateTest`, quick mode, run in scratch copies outside the repo:
  - At 2dcb46e (fixed): about 284 million samples, all `2`. Passed.
  - At 9c68c5f (original): `1` in 495,927 of about 379 million samples (0.13%). Failed, as
    it should.

Committing the jcstress test before the fix was a good order. It let you watch it fail
first.

### Findings

**1. "Writes to `views` are atomic" is not the guarantee you need. (Medium)**

- A single write to a volatile `long` is already atomic (JLS 17.7). The original code had
  that, and it still lost updates.
- Say exactly which sequence must be atomic, and with respect to what.
- The class has a second reader, the reporting thread in `total()`, which takes no lock. The
  guarantee should also say what makes the latest count visible to it. Name the rule.

**2. "Why it is intermittent: It's not" is wrong. (Medium)** This is the same point as
LatencyStats finding 5, and here your own test proves it. On the original code, jcstress lost
an update in 0.13% of samples. The stress test fails on every run only because each round makes
800,000 increments, which gives the race thousands of chances. Say what decides whether a given
pair of increments collides.

**3. "Fix and verification" doesn't say how you verified. (Medium)** Record how many stress
test runs you made, and the jcstress results against both the original and the fix.

**4. Minor points. (Minor)**

- The file is titled "LatencyStats: diagnosis".
- "Protected by: not protected properly" should say what the field actually had: `volatile`,
  which gives visibility only.
- The table leaves out that `record()` also reads `views`. The read inside `views++` is the
  read that races.
- `PageViewCounterLostUpdateTest` imports `D_Result` but never uses it.

### Questions

Answer these in an "After review" section of `DIAGNOSIS.md`, together with your response to
the findings.

1. Now that `record()` is synchronized, is `volatile` on `views` still needed? What would
   break without it, and could any test show that on your machine?
2. Compare `synchronized`, `AtomicLong.incrementAndGet()` and `LongAdder` for this counter,
   which has many writers and an occasional reader. Which would you choose, and what would you
   measure in week 2 to back the choice?
3. The plan reserves jcstress for bugs a plain stress test can't show reliably, and your
   stress test failed on every run. What did the jcstress test add here, if anything?

## 2026-10-10, Final word

This responds to the "After review" section of `DIAGNOSIS.md` in 53681d2. The same commit
removed `volatile` from `views` and the unused import.

**Verdict (updated).** Diagnosis: correct, but the After review adds a wrong claim (point 1
below). Fix: **incorrect**, down from correct. Removing `volatile` in 53681d2 broke
`total()`.

**Results at 53681d2.**

- `PageViewCounterStressTest`: passed 5 out of 5 runs.
- `PageViewCounterLostUpdateTest`: still passes.
- Neither test can see the new bug, which is the point of question 1. To check, I ran a
  throwaway jcstress Termination test in a scratch copy outside the repo: an actor spins on
  `while (counter.total() == 0)` while the signal calls `record()` once.
  - At 53681d2 the reporter never saw the update in 2 forks (STALE).
  - With `volatile` restored, it terminated in all 2,912 trials.

### Your answers

**1. "These three actions need to be atomic so that the writes are visible to readers":
wrong.** Atomicity and visibility are separate guarantees, and this sentence is exactly what
led to the regression.

- `synchronized` on `record()` makes increments atomic with respect to each other, and makes
  each increment visible to the next thread that takes the same lock.
- `total()` takes no lock, so nothing orders its read after any write in `record()`. The JMM
  then allows the reporting thread to see a stale value, possibly forever, and also allows a
  torn 64-bit read (JLS 17.7).
- At 2dcb46e, `volatile` was what made `total()` correct: the volatile variable rule. That was
  the rule finding 1 asked you to name.

**2. "The edge is the `Future` handoff": right edge, wrong place.** That explains why the
*stress test* sees the final count: its main thread reads after `Future.get()`. A reporting
thread in production calls `total()` "at any time", with no `Future` involved.

**3. Intermittency: conceded, and right.**

**4. Fix and verification: partly addressed.** You say both tests fail without the fix, which
helps. Record how many runs.

**Question 1: wrong, and it contradicts itself.** You say `volatile` isn't needed, then give a
reason it is. Tearing is allowed by the JLS on any JVM, not only 32-bit ones, even though 64-bit
HotSpot doesn't do it in practice. The bigger reason is visibility (point 1). "Can any test show
it on your machine?" is answered above: your two tests can't, and a test with a spinning reader
can.

**Question 2: partly right.** `LongAdder` does scale best for many writers, and `AtomicLong`
uses a CAS rather than a lock. But `synchronized` doesn't necessarily block: the JVM spins
before it parks. And `LongAdder` has costs you left out: `sum()` is slower, and it is not an
atomic snapshot while increments are in flight. The question also asked what you would
measure. That is a JMH throughput benchmark at 1, 2, 4 and 8 writer threads, with and without
a reader, which is exactly week 2's counter benchmark. Hold the ranking until you have the
numbers.

**Question 3: too vague.** For this kata, the jcstress test added three things: a minimal
reproduction (two threads, one increment each), a measured frequency (0.13%), and the README's
contract encoded as FORBIDDEN. The plain stress test already proved the bug, so the jcstress
test was optional here. That's fine, but say so.

### What to study again

- **JCIP 3.1.3, Locking and visibility.** Its central rule is that the reading and writing
  threads must synchronize on a common lock. Gap: point 1 and the regression in 53681d2.
- **JCIP 16.1.3**, the volatile variable rule and the monitor lock rule side by side. Gap: which
  rule covers `total()` in each version of the code.
- **JCIP 3.1.2, Nonatomic 64-bit operations.** Gap: tearing is not specific to 32-bit JVMs.

### Follow-up exercise

Write the test that question 1 asked about. Use a jcstress Termination test with an actor that
spins until `total()` returns 1, and a signal that calls `record()`. Classify STALE against the
README's contract. Run it on 53681d2 and watch it fail. Then fix `total()`, choosing between
`volatile` and `synchronized` and saying why, and run it again.

**This kata is closed.**
