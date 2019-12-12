package org.openl.rules.workspace.dtr.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.LinkedHashMap;

/**
 * Wrapper for properties. Provides a limited amount of methods to access properties file. Keeps the order of properties
 * in the file.
 */
public class PropertiesStorage {
    private ExternalizedProperties properties = new ExternalizedProperties(new LinkedHashMap<>());

    /**
     * Returns keys in properties. Keeps the order in the file.
     */
    public Iterable<Object> keySet() {
        return properties.keySet();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public void load(InputStreamReader in) throws IOException {
        properties.load(in);
    }

    public void store(Writer writer) throws IOException {
        properties.store(writer, null);
    }
}
