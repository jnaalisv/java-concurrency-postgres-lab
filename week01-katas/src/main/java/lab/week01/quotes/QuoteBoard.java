package lab.week01.quotes;

/**
 * Holds the latest quote for an instrument. A market-data thread publishes new quotes;
 * any number of pricing threads read the latest one.
 */
public final class QuoteBoard {

    private Quote latest;

    public void publish(Quote quote) {
        latest = quote;
    }

    /**
     * @return the most recently published quote, or null if none has been published yet
     */
    public Quote latest() {
        return latest;
    }
}
