package de.yanwittmann.gallery.db.dao;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name();

    DatabaseToFieldMapper resultSetToField() default DatabaseToFieldMapper.IDENTITY;

    FieldToDatabaseMapper fieldToResultSet() default FieldToDatabaseMapper.IDENTITY;

    boolean includeInInsert() default true;

    boolean notNull() default false;
}
