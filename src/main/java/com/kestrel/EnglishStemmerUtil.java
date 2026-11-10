
package com.kestrel;

import java.util.Locale;
import org.tartarus.snowball.ext.englishStemmer;

public final class EnglishStemmerUtil {

    private static final ThreadLocal<englishStemmer> STEMMER =
            ThreadLocal.withInitial(englishStemmer::new);

    private EnglishStemmerUtil() {}

    public static String stem(String word) {
        if (word == null || word.isBlank()) {
            return word;
        }

        englishStemmer stemmer = STEMMER.get();
        stemmer.setCurrent(word.toLowerCase(Locale.ROOT));
        stemmer.stem();

        return stemmer.getCurrent();
    }
}
