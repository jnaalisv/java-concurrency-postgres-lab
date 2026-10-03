package lab.week01.rates;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

final class RateTable {

    private final Map<String, BigDecimal> ratesToEur = new HashMap<>();

    void put(String currency, BigDecimal rateToEur) {
        ratesToEur.put(currency, rateToEur);
    }

    BigDecimal rateToEur(String currency) {
        BigDecimal rate = ratesToEur.get(currency);
        if (rate == null) {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
        return rate;
    }
}
