package de.yanwittmann.gallery.media.config;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ConfigConverters {

    public interface ToConfigConverter {
        Object convert(Object value);
    }

    public interface FromConfigConverter<T> {
        T convert(Object value);
    }

    public final static FromConfigConverter<List<String>> STRING_LIST_FROM_CONFIG_CONVERTER = value -> {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return new ArrayList<>();
            if (list.get(0) instanceof String) {
                return (List<String>) value;
            }
        } else if (value instanceof JSONArray) {
            final JSONArray jsonArray = (JSONArray) value;
            final List<String> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                final String str = jsonArray.optString(i);
                if (str != null) list.add(str);
            }
            return list;
        }

        return new ArrayList<>();
    };

    public final static ToConfigConverter LIST_TO_CONFIG_CONVERTER = value -> {
        if (value instanceof Collection) {
            final Collection<?> collection = (Collection<?>) value;
            final JSONArray jsonArray = new JSONArray();
            for (Object str : collection) {
                jsonArray.put(str);
            }
            return jsonArray;
        }

        return new JSONArray();
    };

    public final static FromConfigConverter<?> IDENTITY_FROM_CONFIG_CONVERTER = value -> value;

    public final static ToConfigConverter IDENTITY_TO_CONFIG_CONVERTER = value -> value;
}
