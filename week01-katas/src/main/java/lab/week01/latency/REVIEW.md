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
