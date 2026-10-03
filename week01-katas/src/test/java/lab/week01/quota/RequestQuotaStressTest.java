package lab.week01.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RequestQuotaStressTest {

    private static final int THREADS = 8;
    private static final int LIMIT = 1_000;
    // Together the threads make twice as many requests as the quota allows.
    private static final int REQUESTS_PER_THREAD = 2 * LIMIT / THREADS;
    private static final int ROUNDS = 200;
    private static final String CLIENT = "client-1";

    @Test
    void grantsExactlyLimitRequests() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int round = 1; round <= ROUNDS; round++) {
                RequestQuota quota = new RequestQuota(LIMIT);
                CountDownLatch ready = new CountDownLatch(THREADS);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Integer>> handlers = new ArrayList<>();

                for (int t = 0; t < THREADS; t++) {
                    handlers.add(pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        int granted = 0;
                        for (int i = 0; i < REQUESTS_PER_THREAD; i++) {
                            if (quota.tryAcquire(CLIENT)) {
                                granted++;
                            }
                        }
                        return granted;
                    }));
                }
                ready.await();
                start.countDown();
                int granted = 0;
                for (Future<Integer> handler : handlers) {
                    granted += handler.get(30, TimeUnit.SECONDS);
                }

                assertEquals(LIMIT, granted, "round " + round + ": requests granted");
                assertEquals(granted, quota.used(CLIENT), "round " + round + ": used() vs granted");
            }
        }
    }
}
