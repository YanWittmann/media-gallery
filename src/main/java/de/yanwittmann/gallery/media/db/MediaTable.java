package de.yanwittmann.gallery.media.db;

import de.yanwittmann.gallery.db.dao.JdbcTable;

import java.sql.ResultSet;

public class MediaTable extends JdbcTable<MediaRow, Long> {

    @Override
    protected MediaRow createInstance(ResultSet resultSet) {
        return new MediaRow(resultSet);
    }

    @Override
    public String getSchemaResourcePath() {
        return "db/schema/media.sql";
    }

    @Override
    public String getTableName() {
        return "media";
    }

    @Override
    protected String getPrimaryKeyName() {
        return "id";
    }
}
