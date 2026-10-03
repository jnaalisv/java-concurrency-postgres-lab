package lab.week01.latency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LatencyStatsStressTest {

    private static final int RECORDERS = 4;
    private static final int MONITORS = 2;
    private static final int RECORDS_PER_THREAD = 200_000;
    private static final long LATENCY_MICROS = 250;
    private static final int ROUNDS = 10;

    @Test
    void meanIsAlwaysTheRecordedLatency() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(RECORDERS + MONITORS)) {
            for (int round = 1; round <= ROUNDS; round++) {
                LatencyStats stats = new LatencyStats();
                AtomicInteger recordersRunning = new AtomicInteger(RECORDERS);
                ConcurrentLinkedQueue<Double> wrongMeans = new ConcurrentLinkedQueue<>();
                CountDownLatch ready = new CountDownLatch(RECORDERS + MONITORS);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> tasks = new ArrayList<>();

                for (int t = 0; t < RECORDERS; t++) {
                    tasks.add(pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        for (int i = 0; i < RECORDS_PER_THREAD; i++) {
                            stats.record(LATENCY_MICROS);
                        }
                        recordersRunning.decrementAndGet();
                        return null;
                    }));
                }
                for (int m = 0; m < MONITORS; m++) {
                    tasks.add(pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        while (recordersRunning.get() > 0) {
                            double mean = stats.meanMicros();
                            if (mean != 0.0 && mean != LATENCY_MICROS && wrongMeans.size() < 10) {
                                wrongMeans.add(mean);
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

                assertEquals(List.of(), List.copyOf(wrongMeans), "round " + round + ": means observed while recording");
                assertEquals((long) RECORDERS * RECORDS_PER_THREAD, stats.count(), "round " + round + ": count");
            }
        }
    }
}
