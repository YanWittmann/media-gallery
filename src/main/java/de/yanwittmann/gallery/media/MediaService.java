package de.yanwittmann.gallery.media;

import de.yanwittmann.gallery.db.connection.DatabaseHandler;
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
import java.sql.*;
import java.util.List;
import java.util.StringJoiner;
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
            imageDirectories = configuration.getStringList(ConfigField.IMAGE_DIRECTORIES);
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
                FileWalkerUtils.extensionFilter("jpg", "jpeg", "png", "gif", "mov", "mp4"),
                path -> true,
                file -> processFile(basePathHash, file),
                Math.min(8, Runtime.getRuntime().availableProcessors())
        );

        LOG.info("Finished indexing media directory: {}", mediaDirectory);
    }

    private void deleteMediaWhereBasePath(File basePath) {
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

    public void removeMedia(File file) {
        deleteMediaWhereBasePath(file);
        try {
            final List<String> imageDirectories = configuration.getStringList(ConfigField.IMAGE_DIRECTORIES).stream()
                    .map(File::new)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
            imageDirectories.remove(file.getAbsolutePath());
            configuration.set(ConfigField.IMAGE_DIRECTORIES, imageDirectories);
        } catch (Exception e) {
            LOG.error("Failed to remove media directory from configuration: {}", file.getParentFile().getAbsolutePath(), e);
        }
    }

    public void addMedia(File file) {
        rescanMedia(file);
        try {
            final List<String> imageDirectories = configuration.getStringList(ConfigField.IMAGE_DIRECTORIES).stream()
                    .map(File::new)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
            imageDirectories.add(file.getAbsolutePath());
            configuration.set(ConfigField.IMAGE_DIRECTORIES, imageDirectories);
        } catch (Exception e) {
            LOG.error("Failed to add media directory to configuration: {}", file.getParentFile().getAbsolutePath(), e);
        }
    }

    public void disableMedia(File file) {
        try {
            final List<String> imageDirectories = configuration.getStringList(ConfigField.DISABLED_IMAGE_DIRECTORIES).stream()
                    .map(File::new)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
            imageDirectories.add(file.getAbsolutePath());
            configuration.set(ConfigField.DISABLED_IMAGE_DIRECTORIES, imageDirectories);
        } catch (Exception e) {
            LOG.error("Failed to disable media directory in configuration: {}", file.getParentFile().getAbsolutePath(), e);
        }
    }

    public void enableMedia(File file) {
        try {
            final List<String> imageDirectories = configuration.getStringList(ConfigField.DISABLED_IMAGE_DIRECTORIES).stream()
                    .map(File::new)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
            imageDirectories.remove(file.getAbsolutePath());
            configuration.set(ConfigField.DISABLED_IMAGE_DIRECTORIES, imageDirectories);
        } catch (Exception e) {
            LOG.error("Failed to enable media directory in configuration: {}", file.getParentFile().getAbsolutePath(), e);
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

    public List<Long> getMediaIds(int page, String orderBy, boolean asc, boolean includeVideos) throws SQLException {
        final String primaryOrderBy;
        final String secondaryOrderBy;
        switch (orderBy) {
            case "name":
                primaryOrderBy = "file";
                secondaryOrderBy = "last_edited";
                break;
            case "date":
            default:
                primaryOrderBy = "last_edited";
                secondaryOrderBy = "file";
                break;
        }

        final String effectiveAsc = asc ? "ASC" : "DESC";

        final String whereClauseForDisabledMedia = buildWhereClauseFromDisabledMedia();
        final String whereClauseForVideos = includeVideos ? "" : " file NOT LIKE '%.mp4'";

        final StringJoiner whereClauseJoiner = new StringJoiner(" AND ");
        if (!whereClauseForDisabledMedia.isEmpty()) {
            whereClauseJoiner.add(whereClauseForDisabledMedia);
        }
        if (!whereClauseForVideos.isEmpty()) {
            whereClauseJoiner.add(whereClauseForVideos);
        }

        return this.mediaTable.getByPreparedStatement(connection -> {
                    try {
                        final PreparedStatement statement = connection.prepareStatement("SELECT id FROM " + mediaTable.getTableName() + (whereClauseJoiner.length() > 0 ? " WHERE " + whereClauseJoiner : "") + " ORDER BY " + primaryOrderBy + " " + effectiveAsc + ", " + secondaryOrderBy + " " + effectiveAsc + " LIMIT ? OFFSET ?");
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

    private List<Long> getDisabledMedia() {
        return configuration.getStringList(ConfigField.DISABLED_IMAGE_DIRECTORIES).stream()
                .map(File::new)
                .map(File::getAbsolutePath)
                .map(MediaService::hash)
                .collect(Collectors.toList());
    }

    private String buildWhereClauseFromDisabledMedia() {
        final List<Long> disabledMedia = getDisabledMedia();
        if (disabledMedia.isEmpty()) {
            return "";
        }
        return "base_path_hash NOT IN (" + disabledMedia.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + ")";
    }

    public int getTotalCount(boolean includeVideos) {
        try (final Connection connection = DatabaseHandler.getConnectionProvider().connection()) {

            final String whereClauseForDisabledMedia = buildWhereClauseFromDisabledMedia();
            final String whereClauseForVideos = includeVideos ? "" : " file NOT LIKE '%.mp4'";

            final StringJoiner whereClauseJoiner = new StringJoiner(" AND ");
            if (!whereClauseForDisabledMedia.isEmpty()) {
                whereClauseJoiner.add(whereClauseForDisabledMedia);
            }
            if (!whereClauseForVideos.isEmpty()) {
                whereClauseJoiner.add(whereClauseForVideos);
            }

            final PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + mediaTable.getTableName() + (whereClauseJoiner.length() > 0 ? " WHERE " + whereClauseJoiner : ""));
            final ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get total count of media: " + e.getMessage(), e);
        }
    }

    public int getPageCount(boolean includeVideos) {
        return (int) Math.ceil((double) getTotalCount(includeVideos) / PAGINATION_ENTRIES_PER_PAGE);
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

    public MediaServiceConfiguration getSettings() {
        return configuration;
    }
}
