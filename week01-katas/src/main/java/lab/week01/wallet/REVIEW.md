# Wallet: review

## 2026-10-11, reviewing e0f1898

**Verdict.** Diagnosis: partly correct. Fix: correct.

The CAS loop is the right fix, written correctly: it re-reads the balance on every retry and
refuses as soon as the balance is too low. The jcstress test is a small, exact contract test.
The diagnosis is thinner than your last few:

1. The Mechanism section is one sentence. The interleaving is there, but it's in "Symptom"
   (finding 1).
2. The shared-state row repeats the RequestQuota confusion between a thread-safe object and a
   thread-safe sequence (finding 2).

**Test results.**

- `WalletStressTest` at e0f1898: passed 10 out of 10 runs.
- `WalletOverdraftTest`, quick mode, run in scratch copies outside the repo:
  - At e0f1898: only `1, 0, 40` and `0, 1, 40` were observed. Passed.
  - On the original code (3c1059b): `1, 1, -20` in 7.65% of samples. Failed, as it should.

### Findings

**1. The mechanism is one line, and the interleaving sits under "Symptom". (Medium)** For an
atomicity bug, the mechanism is the step-by-step interleaving that breaks the invariant. You
have already written it in prose under Symptom. Move it to Mechanism and give it numbers. Your
jcstress test already has them: an opening balance of 100 and two withdrawals of 60. Symptom
should say what you actually observed, for example the stress test's "balance went negative"
message, or the `1, 1, -20` outcome.

**2. The shared-state row conflates the field with the object's value. (Medium)**

- "Written by: constructor" is true of the `balanceCents` *reference*, which is final and
  safely published.
- The `AtomicLong`'s *value* is written by `deposit()` and `withdraw()`.
- "The type itself is thread safe" is true of each call and false for the `get()` then
  `addAndGet()` sequence. That is the same point as RequestQuota finding 3. Say which one the
  row means.

**3. Minor points. (Minor)**

- The file is titled "rates: diagnosis".
- "Unit test passes, jcstress test passes" is progress, but add how many runs, and what the
  jcstress test showed on the original.

### Questions

Answer these in an "After review" section of `DIAGNOSIS.md`, together with your response to
the findings.

1. Your loop can retry. Can a single `withdraw()` retry forever? What progress guarantee does
   the CAS loop give, compared with a `synchronized` `withdraw()`?
2. Between the `get()` and the `compareAndSet()`, other threads could move the balance from 100
   to 40 and back to 100. Your CAS would then succeed. Is that ABA situation a bug here? Why or
   why not?
3. `AtomicLong.updateAndGet()` takes a function and could replace the loop. Write the function
   in your head. How would you report "insufficient funds" out of it, and what rule from
   RequestQuota's question 1 applies to that function?
