package de.yanwittmann.gallery.media.db;

import de.yanwittmann.gallery.db.dao.Column;
import de.yanwittmann.gallery.db.dao.DatabaseToFieldMapper;
import de.yanwittmann.gallery.db.dao.JdbcRow;
import de.yanwittmann.gallery.db.dao.PrimaryKey;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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

    private final static int MAX_FILE_PARTS = 4;

    public String getFileForSummary() {
        final List<String> parts = new ArrayList<>();

        File file = getFile();

        for (int i = 0; i < MAX_FILE_PARTS; i++) {
            file = file.getParentFile();
            if (file == null) break;
            parts.add(file.getName());
        }

        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = parts.size() - 1; i >= 0; i--) {
            stringBuilder.append(parts.get(i));
            if (i != 0) stringBuilder.append("/");
        }

        return stringBuilder.toString();
    }

    public File getFile() {
        return new File(file);
    }

    public Timestamp getLastEdited() {
        return last_edited;
    }

    public String getLastEditedAsYYYY_MM_DD() {
        if (last_edited == null) return "unknown";
        return last_edited.toLocalDateTime().toLocalDate().toString();
    }

    public Long getBasePathHash() {
        return base_path_hash;
    }
}
