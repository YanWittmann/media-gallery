package de.yanwittmann.gallery.db.dao;

import de.yanwittmann.gallery.db.connection.DatabaseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract class for JDBC tables.
 *
 * @param <ROW> Type of the row instances.
 * @param <PK>  Type of the primary key.
 */
public abstract class JdbcTable<ROW extends JdbcRow, PK> {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcTable.class);

    protected abstract ROW createInstance(ResultSet resultSet);

    public abstract String getSchemaResourcePath();

    public abstract String getTableName();

    public void createSchema() {
        try (final InputStream is = new ClassPathResource(getSchemaResourcePath()).getInputStream()) {
            DatabaseHandler.getConnectionProvider().executeScript(is);
        } catch (IOException e) {
            throw new RuntimeException("Could not load schema file " + getSchemaResourcePath(), e);
        }
    }

    public void destroySchema() throws SQLException {
        try (Connection connection = DatabaseHandler.getConnectionProvider().connection()) {
            connection.setAutoCommit(false);
            connection.createStatement().execute("DROP TABLE IF EXISTS " + getTableName() + ";");
            connection.commit();
            connection.setAutoCommit(true);
        } catch (Exception e) {
            throw new SQLException("Failed to destroy schema: " + e.getMessage(), e);
        }
    }

    public Optional<ROW> getByPrimaryKey(PK id) throws SQLException {
        this.createSchema();

        try (Connection connection = DatabaseHandler.getConnectionProvider().connection()) {

            final PreparedStatement statement = connection.prepareStatement(String.format("SELECT * FROM %s WHERE %s = ?", getTableName(), getPrimaryKeyName()));
            statement.setObject(1, id);
            final ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(createInstance(resultSet));
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<ROW> getAll() throws SQLException {
        return getBySelectStatement(String.format("SELECT * FROM %s ORDER BY %s", getTableName(), getPrimaryKeyName()));
    }

    protected abstract String getPrimaryKeyName();

    private List<ROW> getBySelectStatement(String selectStatement) throws SQLException {
        this.createSchema();

        try (Connection connection = DatabaseHandler.getConnectionProvider().connection()) {

            final PreparedStatement statement = connection.prepareStatement(selectStatement);
            final ResultSet resultSet = statement.executeQuery();

            return createFromResultSet(resultSet);
        } catch (Exception e) {
            throw new SQLException("Error whilst selecting data from table: " + e.getMessage(), e);
        }
    }

    public List<ROW> getByPreparedStatement(Function<Connection, PreparedStatement> handler) throws SQLException {
        this.createSchema();

        try (Connection connection = DatabaseHandler.getConnectionProvider().connection()) {
            final PreparedStatement statement = handler.apply(connection);
            final ResultSet resultSet = statement.executeQuery();

            return createFromResultSet(resultSet);
        } catch (Exception e) {
            throw new SQLException("Error whilst selecting data from table: " + e.getMessage(), e);
        }
    }

    public List<ROW> createFromResultSet(ResultSet result) throws SQLException {
        final List<ROW> rows = new ArrayList<>();
        while (result.next()) {
            rows.add(createInstance(result));
        }
        return rows;
    }

    public ROW insert(ROW row) throws SQLException {
        this.createSchema();

        try (Connection connection = DatabaseHandler.getConnectionProvider().connection()) {

            final PreparedStatement statement = row.insertStatement(connection);
            final ResultSet resultSet = statement.executeQuery();
            return createFromResultSet(resultSet).get(0);

        } catch (Exception e) {
            throw new SQLException("Error whilst inserting data into table: " + e.getMessage(), e);
        }
    }

    public void update(ROW foundToDo) throws SQLException {
        this.createSchema();

        try (Connection connection = DatabaseHandler.getConnectionProvider().connection()) {

            final PreparedStatement statement = foundToDo.updateStatement(connection);
            if (statement == null) {
                return;
            }
            LOG.info("Executing update statement: {}", statement);
            statement.executeUpdate();

        } catch (Exception e) {
            throw new SQLException("Error whilst updating data in table: " + e.getMessage(), e);
        }
    }

    public void delete(ROW todo) throws SQLException {
        this.createSchema();

        try (Connection connection = DatabaseHandler.getConnectionProvider().connection()) {

            final PreparedStatement statement = todo.deleteStatement(connection);
            statement.executeUpdate();

        } catch (Exception e) {
            throw new SQLException("Error whilst deleting data from table: " + e.getMessage(), e);
        }
    }
}
