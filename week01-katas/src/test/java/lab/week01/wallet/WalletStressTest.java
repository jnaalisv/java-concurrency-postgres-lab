package lab.week01.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class WalletStressTest {

    private static final int THREADS = 8;
    private static final long OPENING_BALANCE_CENTS = 1_000;
    private static final long WITHDRAWAL_CENTS = 10;
    // Together the threads try to withdraw twice the opening balance, so the wallet runs dry.
    private static final int WITHDRAWALS_PER_THREAD = (int) (2 * OPENING_BALANCE_CENTS / WITHDRAWAL_CENTS / THREADS);
    private static final int ROUNDS = 5_000;

    @Test
    void balanceNeverGoesNegativeAndMatchesSuccessfulWithdrawals() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int round = 1; round <= ROUNDS; round++) {
                Wallet wallet = new Wallet(OPENING_BALANCE_CENTS);
                CountDownLatch ready = new CountDownLatch(THREADS);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Integer>> workers = new ArrayList<>();

                for (int t = 0; t < THREADS; t++) {
                    workers.add(pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        int succeeded = 0;
                        for (int i = 0; i < WITHDRAWALS_PER_THREAD; i++) {
                            if (wallet.withdraw(WITHDRAWAL_CENTS)) {
                                succeeded++;
                            }
                        }
                        return succeeded;
                    }));
                }
                ready.await();
                start.countDown();
                long withdrawnCents = 0;
                for (Future<Integer> worker : workers) {
                    withdrawnCents += worker.get(30, TimeUnit.SECONDS) * WITHDRAWAL_CENTS;
                }

                long balance = wallet.balanceCents();
                assertTrue(balance >= 0, "round " + round + ": balance went negative: " + balance);
                assertEquals(OPENING_BALANCE_CENTS - withdrawnCents, balance,
                        "round " + round + ": balance does not match " + withdrawnCents + " cents withdrawn");
            }
        }
    }
}
