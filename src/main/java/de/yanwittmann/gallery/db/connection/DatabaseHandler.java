package de.yanwittmann.gallery.db.connection;

import de.yanwittmann.gallery.MediaGalleryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatabaseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseHandler.class);

    private static DatabaseConnectionProvider connectionProvider = findDatabaseConnectionProvider();

    private static DatabaseConnectionProvider findDatabaseConnectionProvider() {
        try {
            LOG.info("Using sqlite database connection provider");
            return new SqlLiteDatabaseConnectionProvider(MediaGalleryConfig.getDatabaseDirectory());
        } catch (Exception e) {
            LOG.error("Error finding database connection provider", e);
            return null;
        }
    }

    public static void setConnectionProvider(DatabaseConnectionProvider connectionProvider) {
        if (connectionProvider == null) {
            throw new IllegalArgumentException("Connection provider must not be null");
        }

        LOG.info("Setting custom connection provider to {}", connectionProvider.getClass().getSimpleName());
        DatabaseHandler.connectionProvider = connectionProvider;
    }

    public static DatabaseConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }
}
