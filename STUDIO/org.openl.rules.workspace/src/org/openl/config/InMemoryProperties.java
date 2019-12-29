package org.openl.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.env.PropertyResolver;

public class InMemoryProperties extends AbstractPropertiesHolder {

    private final Map<String, String> changes = new HashMap<>();
    private final Set<String> reverts = new HashSet<>();

    public InMemoryProperties(PropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    @Override
    public void setProperty(String key, Object value) {
        if (key == null) {
            return;
        }

        if (value == null) {
            revertProperty(key);
            return;
        }

        changes.put(key, value.toString());
    }

    @Override
    public String getProperty(String key) {
        return changes.containsKey(key) ? changes.get(key) : propertyResolver.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        if (changes.containsKey(key)) {
            return changes.get(key);
        }
        return propertyResolver.getProperty(key, defaultValue);
    }

    @Override
    public void revertProperty(String key) {
        changes.remove(key);
        reverts.add(key);
    }

    @Override
    public void writeTo(File file) throws IOException {
        Properties properties = new Properties();
        if (file.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }

        for (String revert : reverts) {
            properties.remove(revert);
        }

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String defaultValue = propertyResolver.getProperty(key);
            if (value != null && !value.equals(defaultValue)) {
                // Save only changed values
                properties.setProperty(key, value);
            }
        }

        File parent = file.getParentFile();
        if (!parent.mkdirs() && !parent.exists()) {
            throw new FileNotFoundException("Can't create the folder " + parent.getAbsolutePath());
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        }
    }
}
