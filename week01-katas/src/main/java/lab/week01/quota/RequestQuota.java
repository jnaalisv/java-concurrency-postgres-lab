package lab.week01.quota;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Grants each client at most {@code limit} requests until {@link #reset()} is called,
 * for example at the start of each billing period.
 */
public final class RequestQuota {

    private final int limit;
    private final ConcurrentMap<String, Integer> used = new ConcurrentHashMap<>();

    public RequestQuota(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive: " + limit);
        }
        this.limit = limit;
    }

    /**
     * @return true if the request is within the client's quota and has been counted against it
     */
    public boolean tryAcquire(String clientId) {
        boolean[] acquired = {false};
        used.compute(clientId, (_, current) -> {
            int n = current == null ? 0 : current;
            if (n >= limit) {
                return current;
            }
            acquired[0] = true;
            return n + 1;
        });
        return acquired[0];
    }

    public int used(String clientId) {
        return used.getOrDefault(clientId, 0);
    }

    public void reset() {
        used.clear();
    }
}
