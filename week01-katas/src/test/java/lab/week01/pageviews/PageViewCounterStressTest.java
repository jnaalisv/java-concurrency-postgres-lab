package lab.week01.pageviews;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PageViewCounterStressTest {

    private static final int THREADS = 8;
    private static final int VIEWS_PER_THREAD = 100_000;
    private static final int ROUNDS = 20;

    @Test
    void totalEqualsNumberOfRecordedViews() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int round = 1; round <= ROUNDS; round++) {
                PageViewCounter counter = new PageViewCounter();
                CountDownLatch ready = new CountDownLatch(THREADS);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> workers = new ArrayList<>();

                for (int t = 0; t < THREADS; t++) {
                    workers.add(pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        for (int i = 0; i < VIEWS_PER_THREAD; i++) {
                            counter.record();
                        }
                        return null;
                    }));
                }
                ready.await();
                start.countDown();
                for (Future<?> worker : workers) {
                    worker.get(30, TimeUnit.SECONDS);
                }

                assertEquals((long) THREADS * VIEWS_PER_THREAD, counter.total(), "round " + round);
            }
        }
    }
}
