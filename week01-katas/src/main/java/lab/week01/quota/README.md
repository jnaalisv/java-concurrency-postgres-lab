# RequestQuota

Limits how many requests each client may make per period. Request-handling threads call
`tryAcquire(clientId)` concurrently, often for the same client.

Expected, for every client:

- at most `limit` calls to `tryAcquire` return true;
- if at least `limit` calls are made, exactly `limit` of them return true;
- `used(clientId)` equals the number of calls that returned true.

Stress test: `RequestQuotaStressTest`.
