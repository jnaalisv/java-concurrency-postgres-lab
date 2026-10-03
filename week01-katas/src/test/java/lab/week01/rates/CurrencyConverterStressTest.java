package lab.week01.rates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CurrencyConverterStressTest {

    private static final int THREADS = 4;
    private static final int ROUNDS = 50_000;
    private static final Map<String, BigDecimal> RATES = Map.of(
            "EUR", new BigDecimal("1"),
            "USD", new BigDecimal("0.92"),
            "GBP", new BigDecimal("1.17"),
            "SEK", new BigDecimal("0.087"),
            "JPY", new BigDecimal("0.0062"),
            "CHF", new BigDecimal("1.05"),
            "NOK", new BigDecimal("0.085"),
            "DKK", new BigDecimal("0.134"));
    private static final List<String> CURRENCIES = List.copyOf(RATES.keySet());

    @Test
    void fetchesOnceAndEveryCallerGetsCorrectAmount() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int round = 1; round <= ROUNDS; round++) {
                AtomicInteger fetches = new AtomicInteger();
                CurrencyConverter converter = new CurrencyConverter(() -> {
                    fetches.incrementAndGet();
                    return RATES;
                });
                CountDownLatch ready = new CountDownLatch(THREADS);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> callers = new ArrayList<>();

                for (int t = 0; t < THREADS; t++) {
                    String currency = CURRENCIES.get((round + t) % CURRENCIES.size());
                    callers.add(pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        BigDecimal eur = converter.toEur(currency, BigDecimal.TEN);
                        assertEquals(BigDecimal.TEN.multiply(RATES.get(currency)), eur, currency);
                        return null;
                    }));
                }
                ready.await();
                start.countDown();
                for (Future<?> caller : callers) {
                    try {
                        caller.get(30, TimeUnit.SECONDS);
                    } catch (ExecutionException e) {
                        throw new AssertionError("round " + round + ": a caller failed", e.getCause());
                    }
                }

                assertEquals(1, fetches.get(), "round " + round + ": number of fetches");
            }
        }
    }
}
