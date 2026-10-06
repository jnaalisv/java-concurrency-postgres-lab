package lab.week01.latency;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.D_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = "25.0",  expect = ACCEPTABLE_INTERESTING, desc = "50/2 => new count, stale total")
@Outcome(id = "50.0", expect = ACCEPTABLE,             desc = "50/1 => before recording 250")
@Outcome(id = "150.0", expect = ACCEPTABLE,             desc = "(50+250)/2 => after recording 250")
@Outcome(id = "300.0", expect = ACCEPTABLE_INTERESTING, desc = "(50+250)/1 => new total, stale count")
@Outcome(            expect = FORBIDDEN, desc = "unexpected value")
@State
public class LatencyStatsMeanTest {

    final LatencyStats stats = new LatencyStats();

    public LatencyStatsMeanTest() {
        stats.record(50);
    }

    @Actor
    public void record() {
        stats.record(250);
    }

    @Actor
    public void meanMicros(D_Result r) {
        r.r1 = stats.meanMicros();
    }
}