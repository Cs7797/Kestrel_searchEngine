package com.kestrel;

public class Document {
    private final String id;
    private final String title;
    private final String url;
    private final String content;

    public Document(String id, String title, String content,String url ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id cannot be blank");
        }
        this.id = id;
        if (title == null || content == null) {
            throw new IllegalArgumentException("Title or content cannot be null");
        }
        this.title = title;
        this.content = content;
        this.url = url;
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }
    public String getUrl() {
        return this.url;
    }

}

