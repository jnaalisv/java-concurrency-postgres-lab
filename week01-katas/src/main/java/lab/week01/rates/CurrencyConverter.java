package lab.week01.rates;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Converts amounts to euros. Rates are fetched from the {@link RateSource} on first use
 * and then reused for the lifetime of the converter.
 */
public final class CurrencyConverter {

    private final RateSource source;
    private RateTable table;

    public CurrencyConverter(RateSource source) {
        this.source = source;
    }

    public BigDecimal toEur(String currency, BigDecimal amount) {
        return amount.multiply(table().rateToEur(currency));
    }

    private RateTable table() {
        if (table == null) {
            synchronized (this) {
                if (table == null) {
                    RateTable loaded = new RateTable();
                    for (Map.Entry<String, BigDecimal> rate : source.fetchRatesToEur().entrySet()) {
                        loaded.put(rate.getKey(), rate.getValue());
                    }
                    table = loaded;
                }
            }
        }
        return table;
    }
}
