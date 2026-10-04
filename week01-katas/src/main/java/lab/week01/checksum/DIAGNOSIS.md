# ChecksumScanner: diagnosis

## Symptom
Worker thread still running even though we have called `stop` on `ChecksumScanner` and `join` on the worker thread with 
reasonable wait time.

## Failure mode
Visibility (stale read). The main thread writes a field and the worker thread reads it, with no happens-before edge 
between them.

## Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `running` (boolean) | main thread via `stop()` |  worker threads via field access | nothing |

## Mechanism
There is no happens-before edge between writes and reads to field `running`. 

## Why it is intermittent
Fails every time.

## What the fix must guarantee
Reads and writes accessing `running` must not be re-ordered.

## Fix and verification
Mark `running` as `volatile`. I dont see a reason for jcstress, the failure is produced every time.
