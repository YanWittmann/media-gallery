package de.yanwittmann.gallery.media.config;

import de.yanwittmann.gallery.MediaGalleryConfig;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MediaServiceConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MediaServiceConfiguration.class);

    private final File configFile;
    private final Map<ConfigField, Object> config = new HashMap<>();

    private final long saveTimeout = 1000;
    private Timer saveTimer = null;

    protected MediaServiceConfiguration(File configFile) {
        this.configFile = configFile;
    }

    public void set(ConfigField field, Object value) {
        config.put(field, value);
        saveToFile();
    }

    public void reset(ConfigField field) {
        config.remove(field);
        saveToFile();
    }

    public void reset() {
        for (ConfigField field : ConfigField.values()) {
            config.remove(field);
        }
        saveToFile();
    }

    public Object get(ConfigField field) {
        if (config.containsKey(field)) {
            return config.get(field);
        } else {
            return field.getDefaultValue();
        }
    }

    private void saveToFile() {
        if (saveTimer != null) {
            saveTimer.cancel();
        }

        saveTimer = new Timer();
        saveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                saveToFileNow();
            }
        }, saveTimeout);
    }

    private void saveToFileNow() {
        final JSONObject config = new JSONObject();
        for (Map.Entry<ConfigField, Object> entry : this.config.entrySet()) {
            config.put(entry.getKey().getKey(), entry.getKey().getToConfigConverter().convert(entry.getValue()));
        }

        final String configJson = config.toString();
        try {
            FileUtils.writeStringToFile(configFile, configJson, StandardCharsets.UTF_8);
            LOG.info("Saved configuration to file: " + configFile);
        } catch (IOException e) {
            LOG.error("Failed to save configuration to file: " + configFile, e);
        }
    }

    public static MediaServiceConfiguration fromConfigFile(File file) throws IOException {
        final MediaServiceConfiguration mediaServiceConfiguration = new MediaServiceConfiguration(file);

        if (!file.exists()) {
            LOG.info("Configuration file does not exist, creating default configuration");
            mediaServiceConfiguration.saveToFile();
            return mediaServiceConfiguration;
        }

        final String configJson = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        final JSONObject config = new JSONObject(configJson);

        for (ConfigField value : ConfigField.values()) {
            if (config.has(value.getKey())) {
                mediaServiceConfiguration.set(value, value.getFromConfigConverter().convert(config.get(value.getKey())));
            } else {
                mediaServiceConfiguration.set(value, value.getDefaultValue());
            }
        }

        if (mediaServiceConfiguration.saveTimer != null) {
            mediaServiceConfiguration.saveTimer.cancel();
        }

        return mediaServiceConfiguration;
    }

    public static MediaServiceConfiguration constructDefaultInstance() throws IOException {
        final File file = MediaGalleryConfig.getConfigurationFile();
        return fromConfigFile(file);
    }
}
