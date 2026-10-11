package lab.week01.wallet;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
@Outcome(id = {"1, 0, 40", "0, 1, 40"}, expect = ACCEPTABLE,             desc = "Exactly one withdrawal succeeded")
@Outcome(id = "1, 1, -20",              expect = FORBIDDEN, desc = "Both passed the balance check: overdraft")
@Outcome(                               expect = FORBIDDEN,              desc = "Unexpected")
@State
public class WalletOverdraftTest {

    final Wallet wallet = new Wallet(100);

    @Actor
    public void withdraw1(III_Result r) {
        r.r1 = wallet.withdraw(60) ? 1 : 0;
    }

    @Actor
    public void withdraw2(III_Result r) {
        r.r2 = wallet.withdraw(60) ? 1 : 0;
    }

    @Arbiter
    public void balance(III_Result r) {
        r.r3 = (int) wallet.balanceCents();
    }
}