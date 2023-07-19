package de.yanwittmann.gallery.media;

public class Media {

    private String id;
    private String path;
    private long timestamp;

    public Media(String id, String path, long timestamp) {
        this.id = id;
        this.path = path;
        this.timestamp = timestamp;
    }
}
