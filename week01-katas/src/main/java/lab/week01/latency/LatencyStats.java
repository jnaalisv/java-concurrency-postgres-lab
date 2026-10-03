package lab.week01.latency;

/**
 * Aggregates request latencies. Request threads record each latency; a monitoring thread
 * polls the summary values.
 */
public final class LatencyStats {

    private long count;
    private long totalMicros;
    private long maxMicros;

    public synchronized void record(long latencyMicros) {
        if (latencyMicros < 0) {
            throw new IllegalArgumentException("Latency must not be negative: " + latencyMicros);
        }
        count++;
        totalMicros += latencyMicros;
        maxMicros = Math.max(maxMicros, latencyMicros);
    }

    public long count() {
        return count;
    }

    public long maxMicros() {
        return maxMicros;
    }

    public double meanMicros() {
        return count == 0 ? 0.0 : (double) totalMicros / count;
    }
}
