package de.yanwittmann.gallery;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MediaGalleryConfig {

    private final static String RESOURCES_PROPERTIES_FILE = "photo-gallery.properties";

    private final static Properties properties = getProperties();

    public static Properties getProperties() {
        if (properties == null) {
            final Properties properties = new Properties();

            try (final InputStream is = new ClassPathResource(RESOURCES_PROPERTIES_FILE).getInputStream()) {
                properties.load(is);
            } catch (IOException e) {
                throw new RuntimeException("Could not load properties file " + RESOURCES_PROPERTIES_FILE, e);
            }

            return properties;
        }
        return properties;
    }

    public static File getBaseSaveDirectory() {
        return new File(properties.getProperty("gallery.file.base"));
    }

    public static File getConfigurationFile() {
        return new File(properties.getProperty("gallery.file.config"));
    }

    public static File getDatabaseDirectory() {
        return new File(properties.getProperty("gallery.file.db"));
    }

}
