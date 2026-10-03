package lab.week01.wallet;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A prepaid wallet. Amounts are in cents.
 */
public final class Wallet {

    private final AtomicLong balanceCents;

    public Wallet(long openingBalanceCents) {
        if (openingBalanceCents < 0) {
            throw new IllegalArgumentException("Opening balance must not be negative: " + openingBalanceCents);
        }
        this.balanceCents = new AtomicLong(openingBalanceCents);
    }

    public void deposit(long amountCents) {
        requirePositive(amountCents);
        balanceCents.addAndGet(amountCents);
    }

    /**
     * @return true if the amount was taken from the wallet, false if the balance was too low
     */
    public boolean withdraw(long amountCents) {
        requirePositive(amountCents);
        if (balanceCents.get() < amountCents) {
            return false;
        }
        balanceCents.addAndGet(-amountCents);
        return true;
    }

    public long balanceCents() {
        return balanceCents.get();
    }

    private static void requirePositive(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amountCents);
        }
    }
}
