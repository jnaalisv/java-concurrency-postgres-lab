package lab.week01.wordcount;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Counts how often each word occurs across all text added to it. Words are compared
 * case-insensitively.
 */
public final class WordFrequencies {

    private static final Pattern NON_WORD = Pattern.compile("\\W+");

    private final Map<String, Integer> counts = new HashMap<>();

    public void add(String text) {
        for (String word : NON_WORD.split(text.toLowerCase(Locale.ROOT))) {
            if (!word.isEmpty()) {
                counts.merge(word, 1, Integer::sum);
            }
        }
    }

    public int count(String word) {
        return counts.getOrDefault(word.toLowerCase(Locale.ROOT), 0);
    }

    public int distinctWords() {
        return counts.size();
    }
}
