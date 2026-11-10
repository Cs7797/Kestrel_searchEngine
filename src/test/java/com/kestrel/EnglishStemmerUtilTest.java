package com.kestrel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnglishStemmerUtilTest {

    @Test
    void shouldStemRelatedWords() {
        assertEquals("connect", EnglishStemmerUtil.stem("connected"));
        assertEquals("network", EnglishStemmerUtil.stem("networks"));
        assertEquals("attack", EnglishStemmerUtil.stem("attacks"));
        assertEquals("studi", EnglishStemmerUtil.stem("studies"));
    }

    @Test
    void shouldHandleNullAndBlankInput() {
        assertNull(EnglishStemmerUtil.stem(null));
        assertEquals("", EnglishStemmerUtil.stem(""));
    }
}