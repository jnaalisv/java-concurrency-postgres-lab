# Wallet

A prepaid wallet shared by several payment threads. `deposit` adds money, `withdraw`
takes money only if the wallet holds enough and reports whether it did.

Expected, at all times and under any number of concurrent callers:

- the balance never goes below zero;
- the balance equals the opening balance plus deposits minus successful withdrawals.

Stress test: `WalletStressTest`.
