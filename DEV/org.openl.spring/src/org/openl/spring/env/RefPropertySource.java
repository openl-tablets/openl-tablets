package org.openl.spring.env;

import java.util.HashSet;
import java.util.Set;

import org.openl.util.StringUtils;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

public class RefPropertySource extends PropertySource<Object> {

    static final String PROPS_NAME = "References to properties";
    static final String REF = ".$ref";
    private static final int MAX_REF_DEPTH = 2;

    private final PropertySources propertySources;

    public RefPropertySource(PropertySources propertySources) {
        super(PROPS_NAME);
        this.propertySources = propertySources;
    }

    @Override
    public Object getProperty(String name) {
        return getProperty0(name, 1, new HashSet<>());
    }

    private Object getProperty0(String name, int depth, Set<String> visitedRefs) {
        if (name.endsWith(REF) || depth > MAX_REF_DEPTH) {
            return null;
        }
        String subName = name;
        StringBuilder sufix = new StringBuilder();
        int dot = name.length();
        do {
            sufix.insert(0, subName.substring(dot));
            subName = subName.substring(0, dot);
            String ref = subName + REF;
            String refProp = StringUtils.trimToNull(getPropValue(ref, depth, visitedRefs));
            if (refProp != null) {
                if (!visitedRefs.add(refProp)) {
                    // break already visited refs to prevent looping
                    return null;
                }
                return getPropValue(refProp + sufix, depth, visitedRefs);
            }
            dot = subName.lastIndexOf('.');

        } while (dot > 0);
        return null;
    }

    private String getPropValue(String ref, int depth, Set<String> visitedRefs) {
        for (PropertySource<?> propertySource : propertySources) {
            Object value;
            if (propertySource == this) {
                value = ((RefPropertySource) propertySource).getProperty0(ref, ++depth, visitedRefs);
            } else {
                value = propertySource.getProperty(ref);
            }
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
}
