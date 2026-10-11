package lab.week01.rates;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Converts amounts to euros. Rates are fetched from the {@link RateSource} on first use
 * and then reused for the lifetime of the converter.
 */
public final class CurrencyConverter {

    private final RateSource source;
    private volatile RateTable table;

    public CurrencyConverter(RateSource source) {
        this.source = source;
    }

    public BigDecimal toEur(String currency, BigDecimal amount) {
        return amount.multiply(table().rateToEur(currency));
    }

    private RateTable table() {
        RateTable loaded = table;
        if (loaded == null) {
            synchronized (this) {
                loaded = table;
                if (loaded == null) {
                    loaded = new RateTable(source.fetchRatesToEur());
                    table = loaded;
                }
            }
        }
        return loaded;
    }
}
