package lab.week01.pageviews;

/**
 * Counts page views reported by request-handling threads.
 */
public final class PageViewCounter {

    private long views;

    public synchronized void record() {
        views++;
    }

    public long total() {
        return views;
    }
}
