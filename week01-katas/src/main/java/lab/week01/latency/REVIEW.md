# LatencyStats: review

## 2026-10-07, reviewing 0ca109f

**Verdict.** Diagnosis: partly correct. Fix: incomplete.

You picked the right failure mode, and `synchronized` on `meanMicros()` is a correct fix for
the mean. The three most important things to work on:

1. Write the mechanism. That section is empty (finding 1).
2. Make the jcstress test able to fail. As written, it passes on the broken code too
   (finding 2).
3. Decide what the fix must guarantee beyond visibility, and apply it to the other two
   accessors (findings 3 and 4).

**Test results.**

- `LatencyStatsStressTest` at 0ca109f: passed 10 out of 10 runs.
- `LatencyStatsMeanTest`, quick mode, run in scratch copies outside the repo:
  - At 0ca109f (fixed): only `50.0` and `150.0` were observed.
  - With `synchronized` removed from `meanMicros()` (original): about 261 million samples,
    including `25.0` 25,062 times and `300.0` 60,741 times. jcstress reported the test as
    "Interesting", with **no failed tests**.

### Findings

**1. The mechanism section is empty. (Major)** This is the heart of the diagnosis. Write
out, step by step, an interleaving of one recorder and one monitor that produces a wrong
mean. Count how many times `meanMicros()` reads `count`, and in what order it reads the
fields. Then check your interleaving against your own jcstress outcomes: which one does it
produce, `25.0` or `300.0`? Can you construct the other one?

**2. The jcstress test cannot fail. (Major)** `25.0` and `300.0` are classified
`ACCEPTABLE_INTERESTING`, so the test passes whatever the code does. On the original code
jcstress saw both outcomes tens of thousands of times and still reported no failures. The
README defines the contract: the mean is always the mean of some prefix of the recorded
latencies. Classify each outcome against that contract, the way you classified STALE in
`ChecksumScannerTermination`. Then run the test against the original code and the fix, and
confirm it fails on one and passes on the other.

**3. "What the fix must guarantee" names only visibility, and misuses synchronizes-with.
(Major)**

- Synchronizes-with relates specific pairs of actions: an unlock and a later lock of the same
  monitor, or a volatile write and a later volatile read of the same variable (JCIP 16.1.3).
  Plain writes and reads of `count` never synchronize-with anything. Restate the guarantee
  in terms of what `record()` and `meanMicros()` actually do.
- Visibility is not the whole requirement. Experiment: drop `synchronized` from
  `meanMicros()`, make `count` and `totalMicros` volatile instead, and run your jcstress test.
  Predict the result before you look. Whatever you find, the section should name the second
  property the fix needs.

**4. The fix covers only `meanMicros()`. (Medium)**

- `count()` and `maxMicros()` still read shared fields with no lock. The README says the
  monitoring thread calls all three.
- Your table says `maxMicros` is "not read", but `maxMicros()` is public and documented.
- The lesson about 64-bit fields from ChecksumScanner applies to all three fields here.

**5. "Why it is intermittent: It's not" is not right. (Medium)** The stress test fails on
every run because the race window is wide and the monitors poll in a tight loop. That makes
the failure likely, not deterministic. Your own jcstress numbers show it: the bad outcomes
were under 0.03% of samples. Say what decides whether a given read is wrong, and what that
means for a run that happens to pass.

**6. Minor points. (Minor)**

- The table says `totalMicros` and `maxMicros` are written "via `stop()`". There is no
  `stop()`; they are written in `record()`.
- Typos: "threas", "syncronized".

### Questions

Answer these in an "After review" section of `DIAGNOSIS.md`, together with your response to
the findings.

1. The stress test's main thread calls `count()` without a lock, after `task.get()` has
   returned for every task. Is that read safe? If so, name the happens-before edge that makes
   it safe.
2. An alternative design is to keep the three values in an immutable record and replace the
   whole record on every `record()` call, for example through an `AtomicReference`. What does
   that buy the monitoring thread, and what does it cost the recorders?
3. Would `LongAdder` for `count` and `totalMicros` fix the mean? Why or why not?

## 2026-10-09, Final word

This responds to the "After review" section of `DIAGNOSIS.md` in c05a812, which also
synchronizes `count()` and `maxMicros()` and reclassifies the jcstress outcomes.

**Results at c05a812.**

- `LatencyStatsStressTest`: passed 10 out of 10 runs.
- `LatencyStatsMeanTest`, quick mode: passed, with only `50.0` and `150.0` observed.
- With `synchronized` removed from all three accessors, in a scratch copy: the test now
  **fails**. jcstress observed `25.0` 29,752 times and `300.0` 53,105 times, and reported
  both as FORBIDDEN.

**Verdict (updated).** Diagnosis: correct, up from partly correct, apart from one wrong
claim about `volatile` (finding 4 below). Fix: correct. The jcstress test is now a real
contract test.

### Your answers

**Finding 1, mechanism: right, with one arithmetic slip.**

- The interleaving for `25.0` is right: the recorder has written `count` but not yet
  `totalMicros` when the monitor reads them.
- Your `0 / 1` point is a good catch. During the very first `record()`, a torn read gives
  `0.0`, which looks exactly like "nothing recorded yet", so no test can tell it apart.
- `(50 + 250) / 1` is `300.0`, not `150.0`; `150.0` is the correct mean after both records.
- Your instinct about `300.0` is right, and it's the most interesting part of this kata. No
  interleaving of the two threads can produce it. In program order `record()` writes `count`
  before `totalMicros`, so once the new total is visible the new count should be too. That
  outcome exists only because there is no happens-before edge. Without one, the monitor's
  plain reads are not required to see the recorder's writes in program order. So this kata
  has two failure mechanisms: an interleaving (`25.0`) and an ordering effect (`300.0`).
  Write that down without the "seems like".
- A small slip: "without synchronizing the method `totalMicros`" should be `meanMicros()`.

**Finding 3, what the fix must guarantee: right.** The synchronizes-with sentence is now
precise, and you named atomicity of the pair as the second property.

**Finding 4, the other accessors: correct fix, wrong reasoning.** Making `count()` and
`maxMicros()` synchronized is a good choice. But both reasons you give for "volatile won't be
enough" are wrong:

- "Their updates use their previous state": the updates happen inside `record()`, which
  stays synchronized, so the read-modify-write is already atomic. A getter performs one
  read, and a volatile read is enough for one field.
- "Being 64-bit values they could tear": volatile `long` reads and writes are always atomic
  (JLS 17.7, JCIP 3.1.2). Tearing is a reason *for* volatile, not against it.

`volatile` fields with a synchronized `record()` would fix `count()` and `maxMicros()`. Only
`meanMicros()` needs the lock, because it reads two fields that must agree. Fix the
sentence; the code is fine either way.

**Findings 2, 5 and 6: addressed.** The test classification is fixed, and it now fails on
the original code and passes on the fix. On intermittency, "not guaranteed to happen" is
enough. The table's typos and the `stop()` slip are fixed. `maxMicros` still says "not read".

**Question 1: right conclusion, wrong edge.** The stress test's worker threads are not
terminated when the main thread calls `count()`. They belong to a pool that is reused across
rounds and only shut down when the try-with-resources block closes. The thread termination
rule therefore doesn't apply. The edge is the `Future` handoff: actions taken by the task
happen-before `Future.get()` returns in another thread. That is one of the
`java.util.concurrent` memory-consistency guarantees. Since c05a812, `count()` is also
synchronized, so the monitor lock rule covers it too.

**Question 2: right, but incomplete.** Monitors read without a lock, and recorders allocate a
new record every time. The missing cost is on the writer side. Several recorders replacing the
same reference need a compare-and-set loop that retries under contention, or a lock. Week 2's
benchmarks are the place to measure that against `synchronized`.

**Question 3: right.** Two `LongAdder`s can't be read together atomically. Even a single
`sum()` is not an atomic snapshot while updates are in flight.

### What to study again

- **JCIP 2.4, Guarding state with locks**, the rule that every variable in an invariant is
  guarded by the same lock. Gap: why `meanMicros()` needs the lock and the getters don't.
- **JCIP 3.1.2, Nonatomic 64-bit operations**, together with what `volatile` guarantees for
  `long`. Gap: the volatile claim in finding 4.
- **JCIP 16.1.4, Piggybacking on synchronization**, which covers the happens-before guarantees
  of the `java.util.concurrent` classes, including `Future.get()`. Gap: question 1.

### Follow-up exercise

Run the experiment from finding 3: drop `synchronized` from `meanMicros()`, make `count` and
`totalMicros` volatile, and run `LatencyStatsMeanTest`. Before you run it, predict which of
`25.0` and `300.0` survive. Then explain the result using the two mechanisms above.

**This kata is closed.**
