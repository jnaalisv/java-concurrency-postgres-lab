package lab.week01.wordcount;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class WordFrequenciesStressTest {

    private static final int THREADS = 8;
    private static final int DISTINCT_WORDS = 5_000;
    private static final int LINES_PER_THREAD = 20;
    private static final int ROUNDS = 20;

    @Test
    void countsEveryOccurrence() throws Exception {
        // Every line contains each word once, so every word must end up with the same count.
        String line = IntStream.range(0, DISTINCT_WORDS)
                .mapToObj(i -> "word" + i)
                .collect(Collectors.joining(" "));
        int expectedCount = THREADS * LINES_PER_THREAD;

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int round = 1; round <= ROUNDS; round++) {
                WordFrequencies frequencies = new WordFrequencies();
                CountDownLatch ready = new CountDownLatch(THREADS);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> indexers = new ArrayList<>();

                for (int t = 0; t < THREADS; t++) {
                    indexers.add(pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        for (int i = 0; i < LINES_PER_THREAD; i++) {
                            frequencies.add(line);
                        }
                        return null;
                    }));
                }
                ready.await();
                start.countDown();
                for (Future<?> indexer : indexers) {
                    indexer.get(30, TimeUnit.SECONDS);
                }

                assertEquals(DISTINCT_WORDS, frequencies.distinctWords(), "round " + round + ": distinct words");
                for (int i = 0; i < DISTINCT_WORDS; i++) {
                    assertEquals(expectedCount, frequencies.count("word" + i), "round " + round + ": word" + i);
                }
            }
        }
    }
}
