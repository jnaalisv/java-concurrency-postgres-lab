# ChecksumScanner: review

## 2026-10-04, reviewing 278e21d

### Test results

- Fixed version: `ChecksumScannerStressTest` passed 20 out of 20 runs.
- Original version, run 6 times in a scratch copy outside the repo: it failed every
  time, and **always in round 3**. Rounds 1 and 2 passed every time.
- No jcstress test yet.

### Diagnosis

The failure mode is right. The diagnosis stops at naming it, though, and three
sections are missing reasoning or say something inaccurate.

**1. The shared-state table is incomplete (major).** It has one row. Go through every
field in the class, not only the one the test exercises. For each field, ask which
thread writes it, which threads can read it, and what makes the write visible to the
reader. Read the class's Javadoc as well as its README: the Javadoc makes a promise
about one more field. List `data` too, and say why it is safe. "Nothing needed" is an
acceptable answer only if you name the happens-before edge that makes it so.

**2. The mechanism names the missing edge but does not explain the hang (major).**
"No happens-before edge" explains why the worker *may* read a stale value. It does
not explain why the worker *never* stops: the test waits 2 seconds, not
2 microseconds. The mechanism also needs:

- the specific edge that would fix it, written as "action A synchronizes-with
  action B" (JLS 17.4.4);
- what in the JVM turns "may see a stale value" into "never sees the new value".

The JMM permits this outcome, but something concrete has to produce it.

**3. "Fails every time" is not accurate (major).** It fails on every *run*, but never
in the first two *rounds*. Why does identical code stop correctly in rounds 1 and 2
and hang in round 3? This section also has to answer the template's second
question: if the original code had passed every run on some machine, what would that
have proved? To settle both, run the original with `-DargLine=-Xint` and predict the
result before you look.

**4. "Must not be re-ordered" is the wrong property (medium).** Only one variable is
involved, so reordering relative to other memory operations is not the issue. What
the fix must guarantee is visibility. Once `stop()` has written `false`, a later read
of `running` by the worker must see it. Phrase it as a happens-before relationship
between those two actions.

### Fix

**5. `volatile` on `running` is correct and minimal for the README's termination
invariant.** It passed 20 out of 20 runs. Your own explanation of *why* it is correct
belongs in finding 2.

**6. The fix covers only what the test checks.** Once finding 1 is resolved, decide
whether the class keeps every promise its documentation makes. You can't conclude
anything from the stress test here, because it never calls the other accessor.

### Verification

**7. The reason for skipping jcstress doesn't hold (medium).** "The failure is
produced every time" describes one JVM on one machine with one JIT, at one point in
the warm-up. Finding 3 shows it does not even fail in every round. The plan asks for
a jcstress Termination test for the stop flag. Such a test checks across compilation
modes and many trials whether the hang is an outcome the JMM permits, which is the
claim your diagnosis makes. Write one for the original code, watch it report the
stuck outcome, then run it against the fix.

Also record how many runs you made of the fixed version. "Re-run many times" is part
of the deliverable.

### Readability and process

**8. Minor wording and process points.**
- The table says "worker threads". There is one worker thread.
- CLAUDE.md asks for commit prefixes like `<kata>:`. "ChecksumScanner diagnosis" has
  no colon.

### Questions

Answer these in an "After review" section in `DIAGNOSIS.md` or in chat. I'll comment
once you have answered.

1. Rounds 1 and 2 never failed in my runs, and round 3 always did. What changes in
   the JVM between those rounds, and why does it matter for a non-volatile read
   inside a loop?
2. Suppose `stop()` were made `synchronized` and `running` stayed non-volatile, with
   `run()` unchanged. Would the worker be guaranteed to stop? Explain using
   happens-before.
3. Would `AtomicBoolean` work as a fix? Would `Thread.interrupt()` with an
   `isInterrupted()` check work? Which would you choose for this class, and why?
4. After `stop()` returns, the worker may still be partway through a pass. Is that a
   problem for the README's contract? What would a caller need to do to know the
   worker has really finished?
