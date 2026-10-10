# QuoteBoard and Quote: review

## 2026-10-10, reviewing 940d667

**Verdict.** Diagnosis: correct. Fix: correct.

This is your strongest diagnosis so far. The mechanism is a clean JMM argument, it covers
both effects (partially constructed quotes and staleness), and the jcstress test is a proper
contract test that you committed before the fix. What's left is precision:

1. The shared-state table leaves out the field the race goes through (finding 1).
2. "Always observe the latest Quote" is not a guarantee any fix provides (finding 2).
3. Record your verification results (finding 3).

**Test results.**

- `QuoteBoardStressTest` at 940d667: passed 10 out of 10 runs.
- `QuoteBoardPublicationTest`, quick mode, run in scratch copies outside the repo:
  - At 940d667: only `none` and `ACME 100/101` were observed. Passed.
  - On the original code (140fee7): `null 0/0` once and `null 100/101` once, in about 229
    million samples. Failed, as it should.

The original's bad outcomes were very rare under jcstress here. Both partial quotes are worth a
look: the second one has the prices but not the symbol.

### Findings

**1. The shared-state table leaves out `QuoteBoard.latest`. (Medium)** That field is the one
the publishing thread writes and the pricing threads read, and in the original code nothing
protected it. Your mechanism is about exactly that field, but the table lists only `Quote`'s
fields.

**2. "We always observe the latest Quote" overstates the guarantee. (Medium)** No fix makes a
reader see a publish that is still happening at the same moment. Restate it in JMM terms. What
must hold between the `publish()` write and a later `latest()` read? Also say which of your two
changes supplies which part of the guarantee (see question 1).

**3. "Fix and verification" describes a plan, not results. (Medium)** "Verify via unit and
jcstress tests" is what you intended to do. Record what you ran and what you saw, on the
original code and on the fix.

**4. Minor points. (Minor)** The plan asks you to record why code like this is still broken on
x86, where some reorderings may never appear. You're on ARM, where it did appear, so one
sentence is enough. A platform where the test passes doesn't make the code correct, and your
mechanism already explains why.

### Questions

Answer these in an "After review" section of `DIAGNOSIS.md`, together with your response to
the findings.

1. Run your jcstress test twice more: once with only the `final` fields (`latest` plain), and
   once with only `volatile latest` (fields not final). Predict the results first. Then say
   which guarantee from finding 2 each change supplies, and whether your jcstress test can
   tell the two versions apart.
2. Could `Quote` be a record? What would that change, for thread safety and otherwise?
3. Suppose two market-data threads both called `publish()` concurrently. Could anything be
   lost, and does the README's contract care?
