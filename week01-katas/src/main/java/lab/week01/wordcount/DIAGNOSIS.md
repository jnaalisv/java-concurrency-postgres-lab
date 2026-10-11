# wordcount: diagnosis

## Symptom
`add()` throws `ConcurrentModificationException`

## Failure mode
Shared non-thread-safe collection

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `counts` (Map) | `add()` | `distinctWords()`,`count()` | unguarded |


## Mechanism
Lost update on the same word. merge reads the old count and writes the new count, with nothing preventing two threads 
from interleaving.

Lost insert of different words. Two threads write into the map’s internal table at the same time and one write 
overwrites the other. 

Size out of step with contents. size is a separate plain field that both threads increment. It can drift from the 
entries actually in the table, so distinctWords() can report words that don’t exist.

## Why it is intermittent
depends on the interleaving; a passing run proves nothing

## What the fix must guarantee
Multiple threads can concurrently add words.

## Fix and verification
Make `counts` a `ConcurrentHashMap`.

Unit test passes, jcstress test passes.

