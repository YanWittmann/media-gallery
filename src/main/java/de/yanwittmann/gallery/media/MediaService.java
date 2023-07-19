package de.yanwittmann.gallery.media;

import de.yanwittmann.gallery.media.config.ConfigField;
import de.yanwittmann.gallery.media.config.MediaServiceConfiguration;
import de.yanwittmann.gallery.media.db.MediaRow;
import de.yanwittmann.gallery.media.db.MediaTable;
import de.yanwittmann.gallery.util.FileWalkerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

public class MediaService {

    public static final Logger LOG = LoggerFactory.getLogger(MediaService.class);

    private final int PAGINATION_ENTRIES_PER_PAGE = 60;

    private final MediaServiceConfiguration configuration;
    private final MediaTable mediaTable;

    public MediaService() throws IOException {
        this.configuration = MediaServiceConfiguration.constructDefaultInstance();
        this.mediaTable = new MediaTable();

        if (configuration.get(ConfigField.INDEX_ON_STARTUP).equals(true)) {
            rescanAllMedia();
        }
    }

    public void rescanAllMedia() {
        LOG.info("Re-Indexing all media");

        final List<String> imageDirectories;
        try {
            imageDirectories = (List<String>) configuration.get(ConfigField.IMAGE_DIRECTORIES);
        } catch (Exception e) {
            LOG.error("Failed to get image directories from configuration, skipping indexation", e);
            return;
        }

        for (String mediaDirectory : imageDirectories) {
            rescanMedia(new File(mediaDirectory));
        }
    }

    public void rescanMedia(File mediaDirectory) {
        LOG.info("Re-Indexing media directory: {}", mediaDirectory);

        deleteMediaWhereBasePath(mediaDirectory);

        if (!mediaDirectory.exists()) {
            LOG.warn("Media directory does not exist: {}", mediaDirectory);
            return;
        } else if (!mediaDirectory.isDirectory()) {
            LOG.warn("Media directory is not a directory: {}", mediaDirectory);
            return;
        }

        final long basePathHash = hash(mediaDirectory.getAbsolutePath());

        FileWalkerUtils.walkFileTreeMultiThreaded(
                Path.of(mediaDirectory.toURI()),
                FileWalkerUtils.extensionFilter("jpg", "jpeg", "png", "gif"),
                path -> true,
                file -> processFile(basePathHash, file),
                Math.min(8, Runtime.getRuntime().availableProcessors())
        );

        LOG.info("Finished indexing media directory: {}", mediaDirectory);
    }

    public void deleteAllMedia() {
        try {
            this.mediaTable.destroySchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to destroy media table schema: " + e.getMessage(), e);
        }
    }

    public void deleteMediaWhereBasePath(File basePath) {
        try {
            final List<MediaRow> mediaResults = this.mediaTable.getByPreparedStatement(connection -> {
                try {
                    final PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + mediaTable.getTableName() + " WHERE base_path_hash = ?");
                    statement.setLong(1, hash(basePath.getAbsolutePath()));
                    return statement;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            for (MediaRow mediaRow : mediaResults) {
                this.mediaTable.delete(mediaRow);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove all media entries for base path [" + basePath + "]: " + e.getMessage(), e);
        }
    }

    private void processFile(long basePathHash, Path file) {
        final MediaRow mediaRow = new MediaRow();

        mediaRow.setFile(file.toAbsolutePath().toString());
        mediaRow.setId(calculateHashFromPath(file));

        final Timestamp lastModifiedTimestamp = new Timestamp(file.toFile().lastModified());
        mediaRow.setLastEdited(lastModifiedTimestamp);

        mediaRow.setBasePathHash(basePathHash);

        try {
            mediaTable.insert(mediaRow);
        } catch (SQLException e) {
            LOG.error("Failed to insert media file into database: {}", mediaRow.getId(), e);
            throw new RuntimeException("Failed to insert media file into database: " + mediaRow.getId(), e);
        }
    }

    private long calculateHashFromPath(Path file) {
        return hash(file.toAbsolutePath().toString());
    }

    public static long hash(String string) {
        // algorithm from https://stackoverflow.com/a/1660613/11769745
        // adapted from String.hashCode()
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

    public List<Long> getMediaIds(int page) throws SQLException {
        return this.mediaTable.getByPreparedStatement(connection -> {
                    try {
                        final PreparedStatement statement = connection.prepareStatement("SELECT id FROM " + mediaTable.getTableName() + " ORDER BY last_edited DESC LIMIT ? OFFSET ?");
                        statement.setInt(1, PAGINATION_ENTRIES_PER_PAGE);
                        statement.setInt(2, page * PAGINATION_ENTRIES_PER_PAGE);
                        return statement;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }).stream()
                .map(MediaRow::getId)
                .collect(Collectors.toList());
    }

    public int getTotalCount() {
        return this.mediaTable.count();
    }

    public int getPageCount() {
        return (int) Math.ceil((double) getTotalCount() / PAGINATION_ENTRIES_PER_PAGE);
    }

    public File getMediaFile(long id) {
        try {
            return this.mediaTable.getByPrimaryKey(id)
                    .map(MediaRow::getFile)
                    .orElse(null);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get media file for id [" + id + "]: " + e.getMessage(), e);
        }
    }
}
