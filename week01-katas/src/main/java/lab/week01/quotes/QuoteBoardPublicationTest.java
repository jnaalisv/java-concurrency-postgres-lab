package lab.week01.quotes;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.L_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = "none",         expect = ACCEPTABLE,  desc = "Reader ran before publish")
@Outcome(id = "ACME 100/101", expect = ACCEPTABLE,  desc = "Fully constructed quote")
@Outcome(                     expect = FORBIDDEN,   desc = "Partially constructed quote")
@State
public class QuoteBoardPublicationTest {

    final QuoteBoard board = new QuoteBoard();

    @Actor
    public void publisher() {
        board.publish(new Quote("ACME", 100, 101));
    }

    @Actor
    public void reader(L_Result r) {
        Quote q = board.latest();
        r.r1 = (q == null) ? "none" : q.toString();
    }
}