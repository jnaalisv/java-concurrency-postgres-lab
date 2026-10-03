# CurrencyConverter

Converts amounts to euros. Fetching rates is slow, so the converter fetches them lazily,
on the first call to `toEur`, and reuses them afterwards. One converter is shared by many
threads, and the first calls may arrive from several threads at once.

Expected:

- the `RateSource` is asked for rates exactly once per converter;
- every call to `toEur` for a currency the source provided returns the correct amount.

Stress test: `CurrencyConverterStressTest`.
