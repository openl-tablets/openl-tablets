package org.openl.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Aleh Bykhavets
 */
public class ConfigSet {
    public static String REPO_PASS_KEY = "repository.encode.decode.key";
    private final Logger log = LoggerFactory.getLogger(ConfigSet.class);

    private Map<String, Object> properties;

    public ConfigSet() {
        properties = new HashMap<String, Object>();
    }

    public void addProperties(Properties props) {
        Enumeration<?> e = props.propertyNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();
            String value = props.getProperty(key);

            addProperty(key, value);
        }
    }

    public void addProperties(Map<String, Object> props) {
        if (props != null) {
            properties.putAll(props);
        }
    }

    public void addProperty(String name, String value) {
        if (value == null) {
            return;
        }
        // extra spaces is a big problem
        value = value.trim();

        if (value.length() == 0) {
            return;
        }

        properties.put(name, value);
    }

    public void updateProperties(Collection<ConfigProperty<?>> props) {
        for (ConfigProperty<?> prop : props) {
            updateProperty(prop);
        }
    }

    public void updateProperty(ConfigProperty<?> prop) {
        Object objectValue = properties.get(prop.getName());

        if (objectValue == null) {
            return;
        }

        try {
            prop.setTextValue(objectValue.toString());
        } catch (Exception e) {
            log.error("Failed to update ConfigProperty '{}' with value '{}'!", prop.getName(), objectValue, e);
        }
    }

    public void updatePasswordProperty(ConfigProperty<?> prop) {
        Object objectValue = properties.get(prop.getName());

        if (objectValue == null) {
            return;
        }

        String pass = objectValue.toString();
        String passKey = this.getPassKey();
        if (!StringUtils.isEmpty(passKey)) {
            try {
                prop.setTextValue(PassCoder.decode(pass, passKey));
            } catch (Exception e) {
                log.error("Failed to update ConfigProperty '{}' with value '{}'!", prop.getName(), objectValue, e);
            }
        } else {
            prop.setTextValue(pass);
        }
    }

    private String getPassKey() {
        if (this.properties.containsKey(REPO_PASS_KEY)) {
            if (this.properties.get(REPO_PASS_KEY) instanceof String[]) {
                String[] stringMass = (String[]) this.properties.get(REPO_PASS_KEY);
                return stringMass[0];
            } else {
                return (String) this.properties.get(REPO_PASS_KEY);
            }
        }

        return "";
    }
}
