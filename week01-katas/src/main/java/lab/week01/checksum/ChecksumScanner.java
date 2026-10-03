package lab.week01.checksum;

/**
 * Repeatedly computes a checksum over a block of data on a background thread, so that
 * {@link #lastChecksum()} reflects the most recent full pass. Runs until {@link #stop()}.
 */
public final class ChecksumScanner implements Runnable {

    private final byte[] data;
    private boolean running = true;
    private long lastChecksum;

    public ChecksumScanner(byte[] data) {
        this.data = data.clone();
    }

    @Override
    public void run() {
        while (running) {
            long checksum = 17;
            for (byte b : data) {
                checksum = 31 * checksum + b;
            }
            lastChecksum = checksum;
        }
    }

    public void stop() {
        running = false;
    }

    public long lastChecksum() {
        return lastChecksum;
    }
}
