package com.bunbunka.programmernotebook;

public final class Note {
    private final long id;
    private final String title;
    private final String content;
    private final long updatedAt;

    public Note(long id, String title, String content, long updatedAt) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getUpdatedAt() { return updatedAt; }
}
