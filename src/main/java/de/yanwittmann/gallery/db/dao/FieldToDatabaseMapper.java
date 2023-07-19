package de.yanwittmann.gallery.db.dao;

import java.util.function.Function;

public enum FieldToDatabaseMapper {
    IDENTITY(o -> o),
    BYTE_DATA(o -> {
        if (o instanceof ByteData) {
            return ((ByteData) o).getData();
        } else {
            return null;
        }
    });

    private final Function<Object, Object> mapper;

    FieldToDatabaseMapper(Function<Object, Object> mapper) {
        this.mapper = mapper;
    }

    public Function<Object, Object> getMapper() {
        return mapper;
    }
}
