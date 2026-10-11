# Quote: diagnosis

## Symptom
We can observe partially constructed quotes via jcstress test and QuoteBoardStressTest.

## Failure mode
unsafe-publication, visibility (stale read)

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `Quote.symbol` (String) | constructor | `symbol()`, `toString()` | not protected |
| `Quote.bidCents` (long) | constructor | `bidCents()`, `toString()` | not protected |
| `Quote.askCents` (long) | constructor | `askCents()`, `toString()` | not protected |
| `QuoteBoard.latest` (long) | `publish()` | `latest()` | not protected |


## Mechanism
`latest` is a plain field and Quote’s fields aren’t final, so nothing orders the constructor’s field stores before the 
store of the reference. A reader can get a non-null Quote whose fields still hold their default values (null, 0, 0).

That also means the constructor’s validation doesn’t protect you. The constructor guarantees 0 < bid < ask, but a 
reader can still see a Quote with bid = 0, because it sees the object before the constructor’s writes become visible 
to it.

A reader that polls `latest()` in a loop might keep seeing an old quote or null forever, because the field is not 
volatile.

## Why it is intermittent
depends on the interleaving; a passing run proves nothing

## What the fix must guarantee
That only valid Quotes are observed, or no racy reads of Quote fields, This ensured via making the all the fields final

That no stale reads of `QuoteBoard.latest` can happen via calls to `latest()`, this is done by making the field volatile,
which ensures that writes to the field happen-before reads.

## Fix and verification
The JMM gives final fields a special guarantee: any thread that sees a reference to the object sees its final fields 
fully initialized, even when the reference itself was published through a race.

Make all fields in Quote final, make `QuoteBoard.latest` volatile. Verify via unit and jcstress tests that we can not
observe partially constructed objects.

# After review
The original buggy code might partially work on x86 (unsafe-publication) where the constructor field writes would not
get reordered.

### Questions

1. Without volatile stale reads can happen, but the jcstress test wont catch it. Without final fields, I would expect to 
see unsafe publication of Quote but somehow I did not see them.
2. Yes, it would be equal with the fixed version for thread safety.
3. This is equal to a case where the calls are sequential but with very little time if any between them.