# WordFrequencies: review

## 2026-10-11, reviewing 451dad1

**Verdict.** Diagnosis: correct. Fix: correct.

You found three separate effects of sharing a `HashMap`: lost updates, lost inserts, and a size
that drifts from the contents. The fix is right, and the jcstress test is a good contract test.
It even catches exceptions thrown inside the actors. What's left:

1. The mechanism doesn't explain the symptom you actually saw (finding 1).
2. "What the fix must guarantee" names a capability, not a guarantee (finding 2).

**Test results.**

- `WordFrequenciesStressTest` at 451dad1: passed 10 out of 10 runs.
- `WordFrequenciesTest`, quick mode, run in scratch copies outside the repo:
  - At 451dad1: about 35 million samples, all `2, 1, 2`. Passed.
  - On the original code (b12cb6b): many forbidden outcomes. Failed, as it should. `1, 0, 3`
    appeared 22,168 times: a size of 3 when only two distinct words exist. That confirms your
    size-drift point.

### Findings

**1. The mechanism doesn't explain the `ConcurrentModificationException`. (Medium)** Your
Symptom is a CME, but `WordFrequencies` never iterates over the map, and none of your three
mechanisms throws one. Find where it comes from. Read `HashMap.merge` in the JDK 25 source:
what does it compare before and after it calls the remapping function, and what does that
check mean when a second thread modifies the map in between?

**2. "Multiple threads can concurrently add words" is a capability, not a guarantee.
(Medium)** State what must hold. For example: each word's increment is atomic, concurrent
inserts leave the map consistent, and counts are visible once `add()` has returned. Then say
which part of your fix provides each. Switching to `ConcurrentHashMap` alone is not what
makes the increment atomic (see question 1).

**3. Minor points. (Minor)**

- `HashMap` and `Map` are now unused imports.
- The table should list `add()` as a reader too, since `merge` reads the old count.
- "Unit test passes, jcstress test passes": add how many runs, and what jcstress showed on the
  original.

### Questions

Answer these in an "After review" section of `DIAGNOSIS.md`, together with your response to
the findings.

1. Suppose `add()` used `counts.put(word, counts.getOrDefault(word, 0) + 1)` on the same
   `ConcurrentHashMap`. Would it be correct? Which earlier kata would that be?
2. `distinctWords()` calls `size()` while other threads may still be adding. What does
   `ConcurrentHashMap` promise about `size()` under concurrent updates, and does the README
   need more?
3. One `add("cat dog")` updates two keys. Can a concurrent reader see the new `cat` count but
   the old `dog` count from that same line? Does the README allow it? What would it take to
   make each line appear all at once?
