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

## 2026-10-11, Final word

This responds to the "After review" section and the revised diagnosis in 036f0b8. The code is
unchanged since e0f1898, so the earlier test results still stand.

**Verdict (updated).** Diagnosis: correct, up from partly correct. Fix: correct.

### Your answers

**Finding 1, mechanism: fixed.** It now has the interleaving with numbers, and Symptom says what
you observed.

**Finding 2, the shared-state row: better, but "Protected by" now names the bug instead of the
protection.** "`withdraw()` is not thread safe" is the conclusion. The row should state the
facts that lead to it. Each `AtomicLong` call is atomic, and nothing protects the `get()` then
`addAndGet()` sequence in `withdraw()`.

**Finding 3: title fixed.** The verification line is unchanged.

**Question 1: half right.**

- Right: in theory, one `withdraw()` can lose the CAS race indefinitely while other threads keep
  succeeding. The loop is *lock-free*: some thread always makes progress. It is not
  *wait-free*: no particular thread is guaranteed to.
- Wrong: a `synchronized` `withdraw()` does not "return immediately". It blocks until it gets
  the lock. Intrinsic locks aren't fair, so in theory one thread can starve there too.
- The real difference is what happens when a thread is descheduled mid-operation. With a lock,
  everyone waits for it. With CAS, nobody does.

**Question 2: right conclusion, but give the reason.** ABA is harmless here because the balance
is a plain value with no identity or hidden state. A wallet that holds 100 again is
indistinguishable from one that never changed, so the check you made is still valid. ABA bites
when the compared value stands for something else that may have changed meanwhile, such as a
pointer to a reused node.

**Question 3: the flag works, but the rule is the point.** `updateAndGet` and `getAndUpdate`
may re-apply the function when a CAS fails, so the function must be side-effect-free. A flag
set inside it is only safe if it's set on every path. That is exactly RequestQuota's question
1. There is a cleaner version with no side effect at all:
`long before = balance.getAndUpdate(b -> b >= amount ? b - amount : b); return before >= amount;`.

### What to study again

- **JCIP 15.2 and 15.3**, CAS and nonblocking algorithms, including lock-free versus blocking
  progress (week 2 reading). Gap: question 1.
- **JCIP 15.4.4, The ABA problem.** Gap: why it doesn't apply here.

### Follow-up exercise

None needed.

**This kata is closed.**
