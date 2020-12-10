package org.openl.spring.env;

import org.openl.util.StringUtils;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

class RefPropertySource extends PropertySource<Object> {
    static final String PROPS_NAME = "References to properties";
    private static final String REF = ".$ref";
    private final PropertySources propertySources;

    RefPropertySource(PropertySources propertySources) {
        super(PROPS_NAME);
        this.propertySources = propertySources;
    }

    @Override
    public Object getProperty(String name) {
        if (name.endsWith(REF)) {
            return null;
        }
        String subName = name;
        StringBuilder sufix = new StringBuilder();
        int dot = name.length();
        do {
            sufix.insert(0, subName.substring(dot));
            subName = subName.substring(0, dot);
            String ref = subName + REF;
            String refProp = StringUtils.trimToNull(getPropValue(ref));
            if (refProp != null) {
                return getPropValue(refProp + sufix);
            }
            dot = subName.lastIndexOf('.');

        } while (dot > 0);
        return null;
    }

    private String getPropValue(String ref) {
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource.getName().equals(PROPS_NAME)) {
                break;
            }
            Object value = propertySource.getProperty(ref);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
}
