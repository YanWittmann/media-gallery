package de.yanwittmann.gallery.db.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public abstract class JdbcRow {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcRow.class);

    private final Set<String> changedFields = new HashSet<>();

    public JdbcRow() {
    }

    public JdbcRow(ResultSet resultSet) {
        if (resultSet != null) {
            final Map<Field, Column> columns = getFieldsWithColumnAnnotations();

            for (Map.Entry<Field, Column> entry : columns.entrySet()) {
                final Field field = entry.getKey();
                final Column column = entry.getValue();
                field.setAccessible(true);

                try {
                    if (!hasColumn(resultSet, column.name())) {
                        continue;
                    }
                    final Object result = resultSet.getObject(column.name());
                    final Object mapped = column.resultSetToField().getMapper().apply(result);

                    field.set(this, mapped);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

    public abstract String getTableName();

    public void fieldChanged(String fieldName) {
        changedFields.add(fieldName);
        // LOG.info("Field [{}] changed", fieldName);
    }

    public boolean changeField(String fieldName, Object value) {
        try {
            final Field field = getFieldWithColumnName(fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Field [" + fieldName + "] does not exist on class [" + getClass().getSimpleName() + "]");
            }

            final Column column = field.getAnnotation(Column.class);
            if (column.notNull() && value == null) {
                throw new IllegalArgumentException("Column [" + fieldName + "] must not be null on class [" + getClass().getSimpleName() + "]");
            }

            field.setAccessible(true);
            final Object currentValue = field.get(this);
            if (Objects.equals(currentValue, value)) {
                return false;
            }

            field.set(this, value);
            fieldChanged(fieldName);

            return true;
        } catch (Exception e) {
            LOG.error("Could not change field [{}] to value [{}]", fieldName, value, e);
            return false;
        }
    }

    private Map<Field, Column> getFieldsWithColumnAnnotations() {
        final Map<Field, Column> columns = new HashMap<>();

        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                columns.put(field, field.getAnnotation(Column.class));
            }
        }

        return columns;
    }

    private Field getFieldWithColumnName(String columnName) {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                final Column column = field.getAnnotation(Column.class);
                if (column.name().equals(columnName)) {
                    return field;
                }
            }
        }
        return null;
    }

    private Field getPrimaryKeyField() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                return field;
            }
        }
        return null;
    }

    private String buildPlaceholderString(int length) {
        final StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (int i = 0; i < length; i++) {
            joiner.add("?");
        }
        return joiner.toString();
    }

    private String buildVariableString(Collection<Field> fields) {
        final StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (Field field : fields) {
            joiner.add(field.getAnnotation(Column.class).name());
        }
        return joiner.toString();
    }

    private String buildUpdateString(Collection<Field> fields) {
        final StringJoiner joiner = new StringJoiner(", ");
        for (Field field : fields) {
            joiner.add(field.getAnnotation(Column.class).name() + " = ?");
        }
        return joiner.toString();
    }

    private int insertPlaceholderValues(PreparedStatement statement, Map<Field, Column> columns, Set<Field> includeFields) throws IllegalAccessException, SQLException {
        int i = 1;
        for (Field field : includeFields) {
            final Object value = field.get(this);
            final Column column = columns.get(field);

            final Object mappedValue = column.fieldToResultSet().getMapper().apply(value);
            statement.setObject(i, mappedValue);
            // LOG.info("Setting value [{}] to field [{}] with type [{}]", mappedValue, field.getName(), field.getType());
            i++;
        }
        return i;
    }

    public PreparedStatement insertStatement(Connection connection) {
        // LOG.info("Building insert statement for: {}", this);

        try {
            final Map<Field, Column> columns = getFieldsWithColumnAnnotations();

            final Set<Field> includeFields = new LinkedHashSet<>();

            for (Map.Entry<Field, Column> entry : columns.entrySet()) {
                final Field field = entry.getKey();
                final Column column = entry.getValue();
                field.setAccessible(true);

                if (!column.includeInInsert()) {
                    continue;
                }
                if (column.notNull()) {
                    if (field.get(this) == null) {
                        continue;
                    }
                }

                includeFields.add(field);
            }

            final String sql = String.format("INSERT INTO %s %s VALUES %s RETURNING *",
                    getTableName(), buildVariableString(includeFields), buildPlaceholderString(includeFields.size()));
            final PreparedStatement statement = connection.prepareStatement(sql);

            insertPlaceholderValues(statement, columns, includeFields);

            return statement;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PreparedStatement updateStatement(Connection connection) {
        if (changedFields.isEmpty()) {
            LOG.info("No fields changed, not building update statement on: {}", this);
            return null;
        }

        // LOG.info("Building update statement on {} for: {}", changedFields, this);

        try {
            final Map<Field, Column> columns = getFieldsWithColumnAnnotations();

            final Set<Field> includeFields = new LinkedHashSet<>();

            for (Map.Entry<Field, Column> entry : columns.entrySet()) {
                final Field field = entry.getKey();
                final Column column = entry.getValue();
                field.setAccessible(true);

                if (column.notNull()) {
                    if (field.get(this) == null) {
                        continue;
                    }
                }

                if (changedFields.contains(field.getName())) {
                    includeFields.add(field);
                }
            }

            final String sql = String.format("UPDATE %s SET %s WHERE %s = ?", getTableName(), buildUpdateString(includeFields), getPrimaryKeyColumnName());
            final PreparedStatement statement = connection.prepareStatement(sql);

            final int afterPlaceholderIndex = insertPlaceholderValues(statement, columns, includeFields);

            final Field primaryKeyField = getPrimaryKeyField();
            if (primaryKeyField == null) {
                throw new RuntimeException("No primary key field found for: " + this);
            }

            final Column primaryKeyColumn = primaryKeyField.getAnnotation(Column.class);
            primaryKeyField.setAccessible(true);

            final Object primaryKeyValue = primaryKeyField.get(this);
            final Object mappedPrimaryKeyValue = primaryKeyColumn.fieldToResultSet().getMapper().apply(primaryKeyValue);
            statement.setObject(afterPlaceholderIndex, mappedPrimaryKeyValue);

            return statement;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PreparedStatement deleteStatement(Connection connection) {
        // LOG.info("Building delete statement for: {}", this);

        try {
            final String sql = String.format("DELETE FROM %s WHERE %s = ?", getTableName(), getPrimaryKeyColumnName());
            final PreparedStatement statement = connection.prepareStatement(sql);

            final Field primaryKeyField = getPrimaryKeyField();
            if (primaryKeyField == null) {
                throw new RuntimeException("No primary key field found for: " + this);
            }

            final Column primaryKeyColumn = primaryKeyField.getAnnotation(Column.class);
            primaryKeyField.setAccessible(true);

            final Object primaryKeyValue = primaryKeyField.get(this);
            final Object mappedPrimaryKeyValue = primaryKeyColumn.fieldToResultSet().getMapper().apply(primaryKeyValue);
            statement.setObject(1, mappedPrimaryKeyValue);

            return statement;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getPrimaryKeyColumnName() {
        return getPrimaryKeyField().getAnnotation(Column.class).name();
    }
}
