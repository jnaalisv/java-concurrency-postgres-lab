# QuoteBoard and Quote

`QuoteBoard` holds the latest `Quote` for an instrument. One market-data thread calls
`publish` with each new quote; many pricing threads call `latest()` concurrently.

Expected: any non-null quote returned by `latest()` is one that was published, so it has
a symbol and `0 < bid < ask`, exactly as its constructor enforces.

Stress test: `QuoteBoardStressTest`.
