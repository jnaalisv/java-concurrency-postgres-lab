package lab.week01.quotes;

/**
 * A price quote for one instrument. Prices are in cents; the bid is always below the ask.
 */
public final class Quote {

    private String symbol;
    private long bidCents;
    private long askCents;

    public Quote(String symbol, long bidCents, long askCents) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        if (bidCents <= 0 || bidCents >= askCents) {
            throw new IllegalArgumentException("Need 0 < bid < ask, got bid " + bidCents + ", ask " + askCents);
        }
        this.symbol = symbol;
        this.bidCents = bidCents;
        this.askCents = askCents;
    }

    public String symbol() {
        return symbol;
    }

    public long bidCents() {
        return bidCents;
    }

    public long askCents() {
        return askCents;
    }

    @Override
    public String toString() {
        return symbol + " " + bidCents + "/" + askCents;
    }
}
