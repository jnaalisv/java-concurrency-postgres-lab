package lab.week01.quota;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Outcome(id = "1, 0", expect = ACCEPTABLE,  desc = "Acquire, then reset")
@Outcome(id = "1, 1", expect = ACCEPTABLE,  desc = "Reset, then acquire counted in the new period")
@Outcome(id = "1, 2", expect = FORBIDDEN,   desc = "Old period's usage survived the reset")
@Outcome(             expect = FORBIDDEN,   desc = "Unexpected")
@State
public class RequestQuotaResetTest {

    final RequestQuota quota = new RequestQuota(2);

    public RequestQuotaResetTest() {
        quota.tryAcquire("c");   // one request already used in the old period
    }

    @Actor
    public void acquire(II_Result r) {
        r.r1 = quota.tryAcquire("c") ? 1 : 0;
    }

    @Actor
    public void reset() {
        quota.reset();
    }

    @Arbiter
    public void used(II_Result r) {
        r.r2 = quota.used("c");
    }
}