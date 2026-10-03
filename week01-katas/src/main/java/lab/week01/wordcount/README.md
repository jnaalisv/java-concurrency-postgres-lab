# WordFrequencies

Counts word occurrences. Several indexing threads call `add` with lines of text from
different documents into one shared `WordFrequencies`.

Expected: after all `add` calls have returned, `count(w)` equals the total number of
times `w` occurred in all the added text, and `distinctWords()` equals the number of
different words seen.

Stress test: `WordFrequenciesStressTest`.
