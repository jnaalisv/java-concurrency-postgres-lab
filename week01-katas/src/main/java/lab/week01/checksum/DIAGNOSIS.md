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

# After review

## 1. Shared state
| Field | Written by | Read by | Protected by |
|---|---|---|---|
| `running` (boolean) | main thread via `stop()` |  worker threads via field access | nothing |
| `data` (byte[]) | constructor |   `run()` reads it| defensived copy is assigned to a final variable |
| `lastChecksum` (long) | `run()` | `lastChecksum()` accesible to other threads | nothing |
`lastChecksum` seems to suffer from same visibility issue as `running` without `volatile`, there is no
happens-before edge between writes and reads to `lastChecksum`

## 2.
So the JMM permits reordering of actions in absence of happens-before edge, which is the case here.

## 3.
Yes fails every time was not accurate. I meant that the test eventually fails every time, which it does, 
but also which is missing the point. 

Anyways my guess is that JVM optimizes the code after few executions by reordering the instructions, which
surfaces the data race.

## 4. 
Agreed, its about a happens-before edge between writes and reads which guarantees the visibility.

## 5.
It doesn't keep every promise, but for the record, I looked at this from a production code POV, where 
Javadocs are often out of date and unused methods I tend to remove rather than fix. But this is an exercise 
about concurrency, so I missed the point.

## 6. 
I agree. I initially rejected writing jcstress because I wanted to focus on the meat and not the side dishes. jcstress
is completely new to me in every way, so its additional learning burden, but clearly very useful tool.

TODO: write jcstress test.

### Questions

1. The worker would not be guaranteed to stop because synchronizing the method `stop()` 
   would only add happens-before edge between releasing and acquiring the lock on `ChecksumScanner`.

2. `AtomicBoolean` would work the same as `volatile`. `Thread.interrupt()` with an
   `isInterrupted()` check would also work, but that would be a very roundabout way of doing things.
   For fixing the visibility issue on `running`, I would use `volatile`

3. the README's contract says "lastChecksum() reflects the most recent full pass", so it won't break the 
   contract if the caller calls first stop(), then lastChecksum() and gets the most recent full pass 
   even though the current iteration has not yet finished.
