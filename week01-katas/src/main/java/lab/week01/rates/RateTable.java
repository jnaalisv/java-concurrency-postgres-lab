package lab.week01.rates;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

final class RateTable {

    private final Map<String, BigDecimal> ratesToEur;

    RateTable(Map<String, BigDecimal> ratesToEur) {
        this.ratesToEur = Map.copyOf(ratesToEur);
    }

    BigDecimal rateToEur(String currency) {
        BigDecimal rate = ratesToEur.get(currency);
        if (rate == null) {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
        return rate;
    }
}
