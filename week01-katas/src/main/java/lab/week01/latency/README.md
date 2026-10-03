# LatencyStats

Aggregates request latencies. Many request threads call `record`; a monitoring thread
reads `count()`, `meanMicros()` and `maxMicros()` while recording is going on.

Expected: every value the monitoring thread reads describes the latencies recorded so
far. In particular, `meanMicros()` is always the mean of some prefix of the recorded
latencies, so if every recorded latency is the same value `v`, `meanMicros()` returns
either 0 (nothing recorded yet) or exactly `v`.

Stress test: `LatencyStatsStressTest`.
