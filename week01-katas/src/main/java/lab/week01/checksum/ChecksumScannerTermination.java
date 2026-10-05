package lab.week01.checksum;

import org.openjdk.jcstress.annotations.*;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Scanner saw stop() and exited")
@Outcome(id = "STALE",      expect = FORBIDDEN, desc = "Scanner never saw stop(), spun forever")
@State
public class ChecksumScannerTermination {

    final ChecksumScanner scanner = new ChecksumScanner(new byte[] {1, 2, 3, 4});

    @Actor
    public void actor() {
        scanner.run();
    }

    @Signal
    public void signal() {
        scanner.stop();
    }
}
