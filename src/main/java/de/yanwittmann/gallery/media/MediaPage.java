package de.yanwittmann.gallery.media;

import java.util.List;

public class MediaPage {

    private List<Media> media;
    private int start;
    private int limit;

    public MediaPage(List<Media> media, int start, int limit) {
        this.media = media;
        this.start = start;
        this.limit = limit;
    }
}
