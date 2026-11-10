
package com.kestrel;

import java.util.Set;

public final class StopWords {

    private static final Set<String> WORDS = Set.of(
            "a", "an", "the",
            "and", "or", "but", "if",
            "of", "at", "by", "for",
            "with", "about", "against",
            "between", "into", "through",
            "during", "before", "after",
            "above", "below", "to", "from",
            "up", "down", "in", "out",
            "on", "off", "over", "under",
            "again", "further", "then", "once",
            "is", "am", "are", "was", "were",
            "be", "been", "being",
            "have", "has", "had",
            "do", "does", "did",
            "will", "would", "shall", "should",
            "can", "could", "may", "might", "must",
            "this", "that", "these", "those",
            "i", "me", "my", "we", "our",
            "you", "your", "he", "she", "it",
            "they", "them", "their",
            "what", "which", "who", "whom",
            "when", "where", "why", "how",
            "as", "until", "while",
            "not", "no", "nor",
            "all", "any", "both",
            "each", "few", "more", "most",
            "other", "some", "such",
            "only", "own", "same", "so",
            "than", "too", "very",
            "s", "t", "just", "don",
            "now", "here", "there"
    );

    private StopWords() {
        // Utility class; prevent instantiation.
    }

    public static boolean isStopWord(String word) {
        return word != null && WORDS.contains(word);
    }
}
