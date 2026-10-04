# Week 1: bug katas

Eight small classes in `src/main/java/lab/week01`, one package each. Each package has a
README stating what the class is supposed to do, and each class has a plain stress test
in `src/test/java` with the same package.

| Package | Class |
|---|---|
| `pageviews` | `PageViewCounter` |
| `wallet` | `Wallet` |
| `checksum` | `ChecksumScanner` |
| `rates` | `CurrencyConverter` |
| `quotes` | `QuoteBoard`, `Quote` |
| `wordcount` | `WordFrequencies` |
| `quota` | `RequestQuota` |
| `latency` | `LatencyStats` |

None of them is correct when used from several threads. Some stress tests fail on almost
every run; some fail occasionally; some may pass every time on your machine even though
the class is broken. A passing stress test is not a proof of anything.

```bash
mvn -pl week01-katas test                                   # all stress tests
mvn -pl week01-katas test -Dtest=WalletStressTest           # one kata
for i in {1..20}; do mvn -q -pl week01-katas test -Dtest=WalletStressTest || break; done   # many runs
```

The workflow is in `STUDY_PLAN.md` (week 1): run, diagnose in a `DIAGNOSIS.md` in the kata's
package before changing code, fix, re-run many times. Write jcstress tests in `src/main/java` (see the
module POM for how to run them) for the katas a plain stress test cannot settle.
