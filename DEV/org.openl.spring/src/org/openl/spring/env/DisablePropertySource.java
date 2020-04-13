package org.openl.spring.env;

import java.util.prefs.Preferences;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

public class DisablePropertySource extends PropertySource<Preferences> {

    public static final String PROPS_NAME = "Disable properties";
    public static final String PROPS_PREFIX = "_sys_disable_";

    static DisablePropertySource THE;

    private final MutablePropertySources propertySources;

    public DisablePropertySource(MutablePropertySources resolver) {
        super(PROPS_NAME);
        this.propertySources = resolver;
    }

    @Override
    public Object getProperty(String name) {
        if (name.startsWith(PROPS_PREFIX)) {
            for (PropertySource<?> propertySource : propertySources) {
                if (propertySource.getName().equals(PROPS_NAME)) {
                    break;
                }
                Object value = propertySource.getProperty(name.replaceFirst(PROPS_PREFIX, ""));
                if (value != null) {
                    return Boolean.TRUE.toString();
                }
            }
            return Boolean.FALSE.toString();
        }
        return null;
    }
}
