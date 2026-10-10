# RequestQuota: review

## 2026-10-10, reviewing 119d6f7

**Verdict.** Diagnosis: partly correct. Fix: correct.

`ConcurrentHashMap.compute` is a good fix. It makes the whole read, check and update atomic
for each key, without a lock on the whole object. The diagnosis stops too early. The three
most important things to work on:

1. Finish the mechanism, which currently stops halfway through the interleaving (finding 1).
2. Add the race your own jcstress test found, `reset()` against `tryAcquire()` (finding 2).
3. Say what the map does protect. "Not protected" is wrong (finding 3).

**Test results.**

- `RequestQuotaStressTest` at 119d6f7: passed 10 out of 10 runs.
- The same test on the original code, in a scratch copy outside the repo: round 1 granted
  **2000 of 2000** requests against a limit of 1000.
- `RequestQuotaResetTest`, quick mode:
  - At 119d6f7: only `1, 0` and `1, 1` were observed. Passed.
  - On the original code: `1, 2` in 689,004 samples (1.03%). Failed, as it should.

### Findings

**1. The mechanism stops halfway. (Major)** You have both threads read the same value. Carry
it on: what does each thread write, what does each return, and what does `used` end up at? Then
use your interleaving to explain the result above. The original code didn't just let a few
requests past the limit. It granted every single one. What happens to later checks when two
increments collapse into one?

**2. The `reset()` race isn't in the diagnosis. (Major)** Your jcstress test targets
`reset()` racing `tryAcquire()`, and it caught a real bug: in 1% of samples, usage from the
old period survived the reset. That is a second instance of the same failure, and neither the
mechanism nor the shared-state table mentions it. Write that interleaving too.

**3. "Protected by: not protected" is wrong, and so is the failure-mode label. (Medium)**

- `ConcurrentHashMap` does protect each individual call. Every `get`, `put` and `clear` is
  atomic, and a `put` happens-before a later `get` that sees it.
- What nothing protects is the *sequence* of calls.
- "Check-then-act" describes part of it. The plan's list of failure modes has one that
  matches this kata more precisely. Which is it, and why does the distinction matter when you
  choose a fix?

**4. "Not intermittent, it fails all the time" is wrong again. (Medium)** This is the third
kata with the same claim. Your own jcstress test shows the reset race in 1.03% of samples. A
test that fails on every run has many chances per run to hit a narrow window; it doesn't mean
the bug is deterministic. Treat this as settled from now on.

**5. "Fix and verification" doesn't say how you verified, or what each test covers. (Medium)**

- Record the runs and results.
- Say which bug each test targets. The stress test covers concurrent acquires; the jcstress
  test covers reset against acquire.
- Neither tests the original over-granting with two threads at the limit. That's fine, since
  the stress test shows it reliably, but say so.

**6. Minor points. (Minor)** "I have chose" should be "I have chosen". The table should list
`reset()` as a writer through `clear()`. It names `reset()`, but not what it does to the map.

### Questions

Answer these in an "After review" section of `DIAGNOSIS.md`, together with your response to
the findings.

1. Your remapping function writes to `acquired[0]` as a side effect. Is that safe with
   `ConcurrentHashMap`? The field is declared as `ConcurrentMap`. Would it still be safe if
   someone swapped in a `ConcurrentSkipListMap`? Read the `compute` Javadoc of both before
   answering.
2. Compare a `synchronized` `tryAcquire()` with your `compute` version, for many clients with
   light traffic and for one very busy client. Where does each one serialise?
3. `reset()` calls `clear()` while other threads keep acquiring for many different clients.
   Is the reset atomic across clients? Does the README's contract require it to be?
