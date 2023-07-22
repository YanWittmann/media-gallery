package de.yanwittmann.gallery.media;

import de.yanwittmann.gallery.MediaGalleryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
public class CleanupComponent {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupComponent.class);

    @PreDestroy
    public void cleanup() {
        LOG.info("Cleaning up thumbnails");
        final File[] files = MediaGalleryConfig.getThumbsDir().listFiles();
        if (files == null) return;
        for (File file : files) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                LOG.error("Error deleting file", e);
            }
        }
    }
}

