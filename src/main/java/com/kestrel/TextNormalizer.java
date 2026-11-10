
package com.kestrel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TextNormalizer {

    private TextNormalizer() {}

    public static List<String> tokenize(String content) {
        List<String> terms = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return terms;
        }

        StringBuilder word = new StringBuilder();

        content.codePoints().forEach(codePoint -> {
            if (Character.isLetterOrDigit(codePoint)) {
                word.appendCodePoint(codePoint);
            } else if (!word.isEmpty()) {
                terms.add(word.toString().toLowerCase(Locale.ROOT));
                word.setLength(0);
            }
        });

        if (!word.isEmpty()) {
            terms.add(word.toString().toLowerCase(Locale.ROOT));
        }

        return terms;
    }

    // Normalize an individual term for index lookup.
    public static String normalizeTerm(String term) {
        if (term == null || term.isBlank()) {
            return "";
        }

        return EnglishStemmerUtil.stem(
                term.toLowerCase(Locale.ROOT)
        );
    }

    // Preserve all terms, including stopwords, for phrase indexing.
    public static List<String> normalizeForIndex(String content) {
        return tokenize(content).stream()
                .map(TextNormalizer::normalizeTerm)
                .toList();
    }

    // Remove stopwords before stemming and remove duplicate query terms.
    public static List<String> normalizeQuery(String query) {
        Set<String> normalized = new LinkedHashSet<>();

        for (String term : tokenize(query)) {
            if (!StopWords.isStopWord(term)) {
                normalized.add(normalizeTerm(term));
            }
        }

        return new ArrayList<>(normalized);
    }

    // Count meaningful terms using the original surface forms.
    public static int searchableLength(String content) {
        int length = 0;

        for (String term : tokenize(content)) {
            if (!StopWords.isStopWord(term)) {
                length++;
            }
        }

        return length;
    }
}
