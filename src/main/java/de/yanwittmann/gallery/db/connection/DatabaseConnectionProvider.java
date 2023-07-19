package de.yanwittmann.gallery.db.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.function.Function;

public interface DatabaseConnectionProvider {

    Connection connection() throws SQLException;

    default ResultSet performQueryByPreparedStatement(Function<Connection, PreparedStatement> handler) throws SQLException {
        try (final Connection connection = this.connection()) {
            return handler.apply(connection).executeQuery();
        } catch (Exception e) {
            throw new SQLException("Error whilst executing query: " + e.getMessage(), e);
        }
    }

    default void executeScript(InputStream is) {
        try (final Connection connection = this.connection()) {
            connection.setAutoCommit(false);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String sqlLine;
                while ((sqlLine = br.readLine()) != null) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(sqlLine);
                    }
                }
            } catch (IOException ex) {
                throw new SQLException("Failed to execute SQL script", ex);
            }

            connection.commit();
            connection.setAutoCommit(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute script: " + e.getMessage(), e);
        }
    }
}
