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

## 2026-10-11, Final word

This responds to the "After review" section and the revised diagnosis in 9e658a7. The code is
unchanged since 940d667, so the earlier test results still stand.

**Verdict (updated).** Diagnosis: correct. Fix: correct. Unchanged.

### Your answers

**Finding 1, the table: fixed, with a slip.** `QuoteBoard.latest` is now listed, but its type
is `Quote`, not `long`.

**Finding 2, the guarantee: better, but each change is credited with less than it does.**

- "Writes happen-before reads" needs one word: a volatile write happens-before every
  *subsequent* read of that field, meaning later in the synchronization order. It does not
  reach a read that came before it. That is the precise form of "no stale reads".
- `volatile latest` alone *also* rules out partially constructed quotes. The constructor's
  writes come before the volatile write in program order, and the reader's field reads come
  after the volatile read. By transitivity the constructor happens-before the field reads.
  That is safe publication.
- `final` fields alone rule out partial quotes even under a racy publication (initialization
  safety), but they do nothing for staleness.
- So for valid quotes, either change is enough; for freshness, only `volatile` works. Keeping
  both is a good choice: `final` keeps `Quote` safe however someone publishes it in future.

**Finding 3, verification: conceded in part, as I said in chat.** "Symptom" does cover what you
saw on the original. The results after the fix are still not recorded.

**Finding 4, x86: half right.** Your sentence describes what x86 hardware tends to do, but not
why the code is still broken there. Two reasons:

- The JMM allows the outcome whatever the CPU does, and correctness has to rest on the JMM.
- The JIT may reorder the stores itself, even on x86.

"Might partially work" is the platform hiding the bug, not the code being correct.

**Question 1: half right, and the puzzle has an answer.** You're right that the jcstress test
can't catch staleness. The reason you didn't see partial quotes with only `volatile` is the
safe-publication edge in finding 2: `volatile latest` alone is already enough for that
guarantee. So your jcstress test passes with either change and can't tell them apart.

**Question 2: right on thread safety.** A record's components are private final fields, so
initialization safety is the same. The other half of the question was about what else
changes. A record generates `equals`, `hashCode` and `toString`, moves validation into a
compact constructor, and can't be extended.

**Question 3: right.** Each `publish()` is a single volatile write with no read-modify-write,
so nothing is lost. The last write in synchronization order wins, and the README only
promises that a quote was published.

### What to study again

- **JCIP 3.5, Safe publication**, especially the list of safe publication idioms, which
  includes storing the reference in a volatile field. Gap: finding 2 and question 1.
- **JCIP 16.2.2 and 16.3**, safe publication versus initialization safety. Gap: what `final`
  gives you that `volatile` doesn't, and the reverse.

### Follow-up exercise

Optional: the same freshness test as in PageViewCounter's final word. Write a jcstress
Termination test whose reader spins until `latest()` is non-null, while the signal publishes
one quote. Run it with only `final` fields, then with only `volatile latest`.

**This kata is closed.**
