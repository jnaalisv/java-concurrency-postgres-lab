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
