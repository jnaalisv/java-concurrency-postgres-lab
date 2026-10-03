package lab.week01.quotes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class QuoteBoardStressTest {

    private static final int READERS = 7;
    private static final int QUOTES_PER_ROUND = 1_000_000;
    private static final int ROUNDS = 10;

    @Test
    void readersOnlySeeValidQuotes() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(READERS + 1)) {
            for (int round = 1; round <= ROUNDS; round++) {
                QuoteBoard board = new QuoteBoard();
                AtomicBoolean publishing = new AtomicBoolean(true);
                ConcurrentLinkedQueue<String> invalid = new ConcurrentLinkedQueue<>();
                CountDownLatch ready = new CountDownLatch(READERS + 1);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> tasks = new ArrayList<>();

                tasks.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    for (long bid = 1; bid <= QUOTES_PER_ROUND; bid++) {
                        board.publish(new Quote("ACME", bid, bid + 1));
                    }
                    publishing.set(false);
                    return null;
                }));
                for (int r = 0; r < READERS; r++) {
                    tasks.add(pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        while (publishing.get()) {
                            Quote quote = board.latest();
                            if (quote == null) {
                                continue;
                            }
                            String symbol = quote.symbol();
                            long bid = quote.bidCents();
                            long ask = quote.askCents();
                            if (symbol == null || bid <= 0 || bid >= ask) {
                                invalid.add("symbol=" + symbol + " bid=" + bid + " ask=" + ask);
                            }
                        }
                        return null;
                    }));
                }
                ready.await();
                start.countDown();
                for (Future<?> task : tasks) {
                    task.get(60, TimeUnit.SECONDS);
                }

                assertEquals(List.of(), List.copyOf(invalid), "round " + round + ": invalid quotes observed");
            }
        }
    }
}
