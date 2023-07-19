package de.yanwittmann.gallery.db.dao;

import de.yanwittmann.gallery.MainController;

import java.util.function.Function;

/**
 * PostgreSQL data types to Java types as returned by the JDBC driver.
 */
public enum DatabaseToFieldMapper {
    IDENTITY(o -> o),
    BYTE_DATA(o -> {
        if (o instanceof byte[]) {
            return new ByteData((byte[]) o);
        } else if (o instanceof ByteData) {
            return o;
        }else if (o instanceof String) {
            return new ByteData((String) o);
        } else {
            MainController.LOG.warn("Failed to map database type to field type: {}", o.getClass());
            return null;
        }
    }),
    TIMESTAMP(o -> {
        if (o instanceof java.sql.Timestamp) {
            return o;
        } else if (o instanceof java.sql.Date) {
            return new java.sql.Timestamp(((java.sql.Date) o).getTime());
        } else if (o instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) o).getTime());
        } else if (o instanceof Long) {
            return new java.sql.Timestamp((Long) o);
        } else if (o instanceof String) {
            return java.sql.Timestamp.valueOf((String) o);
        } else {
            MainController.LOG.warn("Failed to map database type to field type: {}", o.getClass());
            return null;
        }
    })
    ;

    private final Function<Object, Object> mapper;

    DatabaseToFieldMapper(Function<Object, Object> mapper) {
        this.mapper = mapper;
    }

    public Function<Object, Object> getMapper() {
        return mapper;
    }
}
