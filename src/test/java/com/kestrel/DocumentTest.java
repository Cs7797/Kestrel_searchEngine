package com.kestrel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {
    @Test 
    void validDocumentShouldStoreItsFields() {
        Document doc1 = new Document("a12", "Java", "Multithreading,concurrency", "");
        String id = doc1.getId();
        String title = doc1.getTitle();
        String content = doc1.getContent();
        String url = doc1.getUrl();
        assertEquals("a12", id);
        assertEquals("Java", title);
        assertEquals("Multithreading,concurrency", content);
        assertEquals("", url);
    }
    
    @Test 
    void blankIdShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new Document(" ", "jva", "dsdsdsdss", null));
    }
    @Test
void nullIdShouldThrowException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Document(null, "Java", "Some content", null)
    );
}

@Test
void nullTitleShouldThrowException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Document("a12", null, "Some content", null)
    );
}

@Test
void nullContentShouldThrowException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Document("a12", "Java", null, null)
    );
}

@Test
void emptyContentShouldBeAllowed() {
    Document doc = new Document("a12", "Java", "", null);

    assertEquals("", doc.getContent());
}

@Test
void nullUrlShouldBeAllowed() {
    Document doc = new Document("a12", "Java", "Some content", null);

    assertEquals(null, doc.getUrl());
}
}
