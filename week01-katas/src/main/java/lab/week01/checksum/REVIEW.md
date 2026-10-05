# ChecksumScanner: review

## 2026-10-04, reviewing 278e21d

**Verdict.** Diagnosis: partly correct. Fix: incomplete.

You named the right failure mode, and `volatile` on `running` is the right, minimal fix
for the README's termination invariant (20 out of 20 runs passed). The three most
important things to work on:

1. Complete the shared-state table. One field is missing, and the fix inherits the gap
   (findings 1 and 5).
2. Explain why the worker *never* stops and why it hangs only from round 3 onwards
   (findings 2 and 3).
3. Write the jcstress Termination test (finding 6).

**Test results.** Fixed version: `ChecksumScannerStressTest` passed 20 out of 20 runs.
Original version, run 6 times in a scratch copy outside the repo: it failed on every
run, and always in round 3. Rounds 1 and 2 passed every time. There is no jcstress
test yet.

### Findings

**1. The shared-state table is incomplete. (Major)** It has one row. Go through every
field in the class, not only the one the test exercises. For each field, ask which
thread writes it, which threads can read it, and what makes the write visible to the
reader. Read the class's Javadoc as well as its README: the Javadoc makes a promise
about one more field. List `data` too, and say why it is safe. "Nothing needed" is an
acceptable answer only if you name the happens-before edge that makes it so.

**2. The mechanism names the missing edge but does not explain the hang. (Major)**
"No happens-before edge" explains why the worker *may* read a stale value. It does not
explain why the worker *never* stops: the test waits 2 seconds, not 2 microseconds.
The mechanism also needs:

- the specific edge that would fix it, written as "action A synchronizes-with action B"
  (JLS 17.4.4);
- what in the JVM turns "may see a stale value" into "never sees the new value".

The JMM permits this outcome, but something concrete has to produce it.

**3. "Fails every time" is not accurate. (Major)** It fails on every *run*, but never
in the first two *rounds*. Why does identical code stop correctly in rounds 1 and 2
and hang in round 3? This section also has to answer the template's second question:
if the original code had passed every run on some machine, what would that have
proved? Experiment: run the original with `-DargLine=-Xint`, predicting the result
before you look.

**4. "Must not be re-ordered" is the wrong property. (Medium)** Only one variable is
involved, so reordering relative to other memory operations is not the issue. What the
fix must guarantee is visibility: once `stop()` has written `false`, a later read of
`running` by the worker must see it. Phrase it as a happens-before relationship
between those two actions.

**5. The fix covers only what the test checks. (Medium)** Once finding 1 is resolved,
decide whether the class keeps every promise its documentation makes. You can't
conclude anything from the stress test here, because it never calls the other
accessor.

**6. The reason for skipping jcstress doesn't hold. (Medium)** "The failure is produced
every time" describes one JVM on one machine with one JIT, at one point in the warm-up.
Finding 3 shows it does not even fail in every round. The plan asks for a jcstress
Termination test for the stop flag. Such a test checks, across compilation modes and
many trials, whether the hang is an outcome the JMM permits, which is exactly the claim
your diagnosis makes. Write one for the original code and watch it report the stuck
outcome, then run it against the fix. Also record how many runs you made of the fixed
version: "re-run many times" is part of the deliverable.

**7. Minor wording and process points. (Minor)**

- The table says "worker threads". There is one worker thread.
- CLAUDE.md asks for commit prefixes like `<kata>:`. "ChecksumScanner diagnosis" has no
  colon.

### Questions

Answer these in an "After review" section of `DIAGNOSIS.md`, together with your
response to the findings.

1. Suppose `stop()` were made `synchronized` and `running` stayed non-volatile, with
   `run()` unchanged. Would the worker be guaranteed to stop? Explain using
   happens-before.
2. Would `AtomicBoolean` work as a fix? Would `Thread.interrupt()` with an
   `isInterrupted()` check work? Which would you choose for this class, and why?
3. After `stop()` returns, the worker may still be partway through a pass. Is that a
   problem for the README's contract? What would a caller need to do to know the worker
   has really finished?

## 2026-10-05, Final word

This responds to the "After review" section of `DIAGNOSIS.md`, committed in a2b40a9.
`ChecksumScanner.java` is unchanged since 278e21d, so the earlier test results still stand.

**Verdict (updated).** Diagnosis: still partly correct. The shared-state table is now nearly
complete, but the hang is still explained as reordering, and that is not what happens here.
Fix: still incomplete, because `lastChecksum` is unchanged. jcstress: not written yet. You
asked me to review it separately once it is.

### Your answers

**Finding 1, shared state: mostly right.**

- The `lastChecksum` row is right: the worker writes it, any thread can read it, and there
  is no happens-before edge between the two. It is incomplete, though. Because of the field's
  type, it has a second hazard that `running` does not have (JCIP 3.1.2).
- The `data` row is right in substance, but "Protected by" mixes two separate guarantees.
  - The defensive copy is confinement: no other code holds a reference to the array, so
    nothing else can write to it while the worker reads it.
  - The `final` field is visibility: it falls under initialization safety, so name that
    rule.
  - This program also has a second, simpler edge that makes `data` visible to the worker.
    Look at how the test starts the worker, then find the matching rule in JCIP 16.1.3.

**Finding 2, mechanism: still wrong.** "The JMM permits reordering" is true of the JMM. It is
not the mechanism here, and it contradicts your own answer to finding 4.

- Reordering means another thread observes two memory operations in a different order from
  program order. An example is seeing a flag before the data it guards, which is exactly
  what your `MessagePassing` sketch tests.
- The worker's loop reads one shared variable, and `stop()` writes that same variable. There
  are no two operations whose order the worker could see swapped.
- What lets the loop spin forever is a different consequence of the missing edge. The JMM
  never requires the worker's reads to see the write at all, and the compiler is entitled to
  rely on that. JCIP 3.1.4 names the specific optimization.
- Still missing from the original finding: the fixing edge stated precisely, meaning which
  action synchronizes-with which, under which rule.

**Finding 3, why it is intermittent: half right.**

- "The JVM optimizes the code after a few executions" points the right way. What changes
  between rounds is how the JIT has compiled `run()`.
- "By reordering the instructions" is wrong, for the reason given under finding 2.
- Two parts are still unanswered: what a passing run would have proved, and the `-Xint`
  experiment, which you don't report having run. The follow-up exercise below covers both.

**Finding 4, visibility rather than reordering: right.**

**Finding 5, the accessor: partly conceded.**

- You're right that deleting an unused accessor is a legitimate fix. With no cross-thread
  read of `lastChecksum` there is no data race on it, and finding 5 should have allowed for
  that.
- It doesn't fit this class, though. `lastChecksum()` is its only output. Without it, the
  scanner computes a value that nobody can see, and the class has no reason to exist. A
  method that is public and documented is also not "unused" just because nothing in this
  repository calls it.
- Either way, you neither fixed the accessor nor removed it, so the fix is still incomplete.

**Finding 6, jcstress: agreed, still to do.**

**Question 1, `synchronized` stop(): right.** The monitor lock rule creates an edge from the
unlock in `stop()` only to a later lock of the same monitor, and `run()` never takes that
lock. To make the answer complete, add that `run()` is unchanged, so nothing stops the
compiler from treating the loop exactly as before.

**Question 2, AtomicBoolean and interruption: half right.**

- AtomicBoolean is right: its `get` and `set` have volatile semantics.
- Interruption is not "roundabout". It is Java's standard cancellation mechanism, and here
  it has consequences. If `ChecksumScanner` runs as a task in an `ExecutorService`, it ignores
  interruption. `shutdownNow()` and `Future.cancel(true)` therefore cannot stop it, and
  `awaitTermination` would time out.
- A flag is a defensible choice for a loop that never blocks. The reason to prefer one over
  the other is a trade-off you'll meet in JCIP 7.1, about who owns the thread, not
  convenience.

**Question 3, stop() while a pass is in flight: wrong.**

- "Gets the most recent full pass" relies on visibility the current code does not have. Your
  own table row says there is no happens-before edge for `lastChecksum`, so the caller may
  read a stale value. The answer contradicts your diagnosis.
- The second half of the question is unanswered: how does a caller know the worker has
  finished? Find the rule in JCIP 16.1.3 that covers a thread terminating. Then work out what
  that rule implies for a call to `lastChecksum()` made after it.
- A smaller point: "reflects the most recent full pass" comes from the class Javadoc. The
  README's contract is about termination.

### What to study again

- **JCIP 3.1.4, Volatile variables**, especially the debugging tip about the server JVM, then
  **16.1.2, Reordering**. Gap: findings 2 and 3, where a visibility failure is explained as
  reordering.
- **JCIP 16.1.3, the list of happens-before rules.** Learn the volatile variable, monitor
  lock, thread start and thread termination rules well enough to name the exact edge each
  time. Gap: the edge for `running`, the `data` row, and question 3.
- **JCIP 3.1.2, Nonatomic 64-bit operations.** Gap: the `lastChecksum` row.
- **JCIP 16.1.1, Platform memory models.** Gap: what a passing run proves. It is the same
  lesson as the plan's note about x86.
- **JCIP 7.1, Task cancellation.** This is week 3 reading, so revisit question 2 then rather
  than now.

If JCIP ch. 16 feels dense, the plan's second explanation of the memory model is WGJD ch. 5.

### Follow-up exercise

Run the original code (`running` not volatile) and find out what changes between round 2 and
round 3. Write down a prediction before each run.

1. `-DargLine=-Xint`: in which round, if any, does it hang?
2. `-DargLine=-XX:-TieredCompilation`: in which round?
3. `-DargLine=-XX:+PrintCompilation`: Surefire saves the JIT log to a `.dumpstream` file in
   `target/surefire-reports`. Find the lines for `ChecksumScanner::run`, and note which are
   marked `%` and which compilation tier each one has. Then line them up with the rounds,
   which run about 300 ms apart.

Record in `notes/week01.md` what you found and why it explains round 3.

**This kata is closed**, except for the jcstress test. You asked me to review that once it is
written, and I'll add the review as a separate dated section.
