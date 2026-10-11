package lab.week01.rates;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.*;

/*
 * Codes for r1 and r2:
 *   1 = correct rate
 *   2 = wrong rate
 *   3 = "Unknown currency": saw the table but not its contents
 *   4 = NullPointerException: second read of table returned null
 * r3 = number of fetches from the RateSource
 */
@JCStressTest
@Outcome(id = "1, 1, 1", expect = ACCEPTABLE, desc = "Both converted correctly, one fetch")
@Outcome(                expect = FORBIDDEN,  desc = "Unsafely published table, or extra fetch")
@State
public class CurrencyConverterLazyInitTest {

    static final BigDecimal USD_RATE = new BigDecimal("0.92");

    final AtomicInteger fetches = new AtomicInteger();

    final CurrencyConverter converter = new CurrencyConverter(() -> {
        fetches.incrementAndGet();
        return Map.of("USD", USD_RATE, "GBP", new BigDecimal("1.17"), "SEK", new BigDecimal("0.088"));
    });

    @Actor
    public void caller1(III_Result r) {
        r.r1 = convert(converter);
    }

    @Actor
    public void caller2(III_Result r) {
        r.r2 = convert(converter);
    }

    @Arbiter
    public void fetches(III_Result r) {
        r.r3 = fetches.get();
    }

    private static int convert(CurrencyConverter c) {
        try {
            return c.toEur("USD", BigDecimal.ONE).compareTo(USD_RATE) == 0 ? 1 : 2;
        } catch (IllegalArgumentException e) {
            return 3;
        } catch (NullPointerException e) {
            return 4;
        }
    }
}