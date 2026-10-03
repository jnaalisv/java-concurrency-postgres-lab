package lab.week01.checksum;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ChecksumScannerStressTest {

    private static final int ROUNDS = 10;
    private static final Duration RUN_TIME = Duration.ofMillis(300);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(2);

    @Test
    void workerTerminatesAfterStop() throws Exception {
        byte[] data = new byte[4096];
        new Random(42).nextBytes(data);

        for (int round = 1; round <= ROUNDS; round++) {
            ChecksumScanner scanner = new ChecksumScanner(data);
            // Daemon, so that a worker that never stops cannot keep the test JVM alive.
            Thread worker = Thread.ofPlatform().daemon().name("checksum-scanner-" + round).start(scanner);

            // Not a synchronisation point: the scanner simply runs for a while, as it would in production.
            Thread.sleep(RUN_TIME);
            scanner.stop();
            worker.join(STOP_TIMEOUT);

            assertFalse(worker.isAlive(),
                    "round " + round + ": worker still running " + STOP_TIMEOUT.toMillis() + " ms after stop()");
        }
    }
}
