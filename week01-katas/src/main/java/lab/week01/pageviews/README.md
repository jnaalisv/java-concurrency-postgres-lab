# PageViewCounter

Counts page views. Many request-handling threads call `record()` once per view, and a
reporting thread calls `total()` at any time.

Expected: after all calls to `record()` have returned, `total()` equals the number of calls.

Stress test: `PageViewCounterStressTest`.
