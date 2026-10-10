# Quote: diagnosis

## Symptom
We can observe partially constructed quotes via jcstress test and QuoteBoardStressTest.

## Failure mode
unsafe-publication, visibility (stale read)

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `symbol` (String) | constructor | `symbol()`, `toString()` | not protected |
| `bidCents` (long) | constructor | `bidCents()`, `toString()` | not protected |
| `askCents` (long) | constructor | `askCents()`, `toString()` | not protected |

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
That only valid Quotes are observed, and that we always observe the latest Quote.

## Fix and verification
The JMM gives final fields a special guarantee: any thread that sees a reference to the object sees its final fields 
fully initialized, even when the reference itself was published through a race.

Make all fields in Quote final, make `QuoteBoard.latest` volatile. Verify via unit and jcstress tests that we can not
observe partially constructed objects.
