# ChecksumScanner

A background task that keeps recomputing a checksum over a block of data. One thread
runs it; another thread later calls `stop()`.

Expected: shortly after `stop()` is called, `run()` returns and the thread terminates.

Stress test: `ChecksumScannerStressTest`.
