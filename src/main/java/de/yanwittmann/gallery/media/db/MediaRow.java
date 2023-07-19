package de.yanwittmann.gallery.media.db;

import de.yanwittmann.gallery.db.dao.Column;
import de.yanwittmann.gallery.db.dao.DatabaseToFieldMapper;
import de.yanwittmann.gallery.db.dao.JdbcRow;
import de.yanwittmann.gallery.db.dao.PrimaryKey;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class MediaRow extends JdbcRow {

    @PrimaryKey
    @Column(name = "id", notNull = true)
    protected Long id;

    @Column(name = "file", notNull = true)
    protected String file;

    @Column(name = "last_edited", notNull = true, resultSetToField = DatabaseToFieldMapper.TIMESTAMP)
    protected Timestamp last_edited;

    @Column(name = "base_path_hash", notNull = true)
    protected Long base_path_hash;

    public MediaRow(ResultSet resultSet) {
        super(resultSet);
    }

    public MediaRow() {
    }

    @Override
    public String getTableName() {
        return "media";
    }

    public void setId(Long id) {
        super.changeField("id", id);
    }

    public void setFile(String file) {
        super.changeField("file", file);
    }

    public void setLastEdited(Timestamp last_edited) {
        super.changeField("last_edited", last_edited);
    }

    public void setBasePathHash(Long base_path_hash) {
        super.changeField("base_path_hash", base_path_hash);
    }

    public Long getId() {
        return id;
    }

    public String getFileAsString() {
        return file;
    }

    public File getFile() {
        return new File(file);
    }

    public Timestamp getLastEdited() {
        return last_edited;
    }

    public Long getBasePathHash() {
        return base_path_hash;
    }
}
