package lab.week01.wordcount;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.III_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

/*
 * r1 = count("cat"), r2 = count("dog"), r3 = distinctWords()
 * r3 = -1 means an actor threw an exception
 */
@JCStressTest
@Outcome(id = "2, 1, 2", expect = ACCEPTABLE,             desc = "All words counted")
@Outcome(                expect = FORBIDDEN, desc = "Lost update, lost insert or corrupted map")
@State
public class WordFrequenciesTest {

    final WordFrequencies frequencies = new WordFrequencies();
    volatile boolean failed;

    @Actor
    public void addCat() {
        try {
            frequencies.add("cat");
        } catch (RuntimeException e) {
            failed = true;
        }
    }

    @Actor
    public void addCatDog() {
        try {
            frequencies.add("Cat dog");
        } catch (RuntimeException e) {
            failed = true;
        }
    }

    @Arbiter
    public void counts(III_Result r) {
        r.r1 = frequencies.count("cat");
        r.r2 = frequencies.count("dog");
        r.r3 = failed ? -1 : frequencies.distinctWords();
    }
}