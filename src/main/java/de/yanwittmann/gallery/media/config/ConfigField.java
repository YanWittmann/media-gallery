package de.yanwittmann.gallery.media.config;

import java.util.ArrayList;

public enum ConfigField {
    IMAGE_DIRECTORIES("image_directories", new ArrayList<>(), ConfigConverters.LIST_TO_CONFIG_CONVERTER, ConfigConverters.STRING_LIST_FROM_CONFIG_CONVERTER),
    INDEX_ON_STARTUP("index_on_startup", true, ConfigConverters.IDENTITY_TO_CONFIG_CONVERTER, ConfigConverters.IDENTITY_FROM_CONFIG_CONVERTER),
    ;

    private final String key;
    private final Object defaultValue;
    private final ConfigConverters.ToConfigConverter toConfigConverter;
    private final ConfigConverters.FromConfigConverter<?> fromConfigConverter;

    ConfigField(String key, Object defaultValue, ConfigConverters.ToConfigConverter toConfigConverter, ConfigConverters.FromConfigConverter<?> fromConfigConverter) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.toConfigConverter = toConfigConverter;
        this.fromConfigConverter = fromConfigConverter;
    }

    public String getKey() {
        return key;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public ConfigConverters.ToConfigConverter getToConfigConverter() {
        return toConfigConverter;
    }

    public ConfigConverters.FromConfigConverter<?> getFromConfigConverter() {
        return fromConfigConverter;
    }
}
