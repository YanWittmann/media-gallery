package de.yanwittmann.gallery.db.connection;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlLiteDatabaseConnectionProvider implements DatabaseConnectionProvider {

    private final String databaseUri;

    public SqlLiteDatabaseConnectionProvider(File databaseFile) {
        if (!databaseFile.getParentFile().exists()) {
            databaseFile.getParentFile().mkdirs();
        }

        this.databaseUri = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
    }

    @Override
    public Connection connection() throws SQLException {
        return DriverManager.getConnection(this.databaseUri);
    }
}
