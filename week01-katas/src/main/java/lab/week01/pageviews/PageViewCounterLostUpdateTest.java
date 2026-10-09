package lab.week01.pageviews;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.J_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = "2", expect = ACCEPTABLE, desc = "Both increments applied")
@Outcome(id = "1", expect = FORBIDDEN,  desc = "Lost update: both read 0, both wrote 1")
@Outcome(          expect = FORBIDDEN,  desc = "Unexpected value")
@State
public class PageViewCounterLostUpdateTest {

    final PageViewCounter counter = new PageViewCounter();

    @Actor
    public void record1() {
        counter.record();
    }

    @Actor
    public void record2() {
        counter.record();
    }

    @Arbiter
    public void total(J_Result r) {
        r.r1 = counter.total();
    }
}