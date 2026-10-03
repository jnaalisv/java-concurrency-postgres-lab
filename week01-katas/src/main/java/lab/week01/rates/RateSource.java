package lab.week01.rates;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Where exchange rates come from, for example a remote service. Fetching is slow, so it
 * should happen once per {@link CurrencyConverter}.
 */
@FunctionalInterface
public interface RateSource {

    /**
     * @return the number of euros one unit of each currency is worth, keyed by ISO 4217 code
     */
    Map<String, BigDecimal> fetchRatesToEur();
}
