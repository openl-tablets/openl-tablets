package org.openl.rules.ruleservice.databinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ServiceDescriptionConfigurationRootClassNamesBindingFactoryBean extends ServiceDescriptionConfigurationFactoryBean<Set<String>> {
    private static final String ROOT_CLASS_NAMES_BINDING = "rootClassNamesBinding";

    private Set<String> defaultAdditionalRootClassNames;

    public void setDefaultAdditionalRootClassNames(Set<String> defaultAdditionalRootClassNames) {
        if (defaultAdditionalRootClassNames == null) {
            throw new IllegalArgumentException("addtionalRootClassNames arg must be not null!");
        }
        this.defaultAdditionalRootClassNames = defaultAdditionalRootClassNames;
    }

    public Set<String> getDefaultAdditionalRootClassNames() {
        return defaultAdditionalRootClassNames;
    }

    @Override
    protected Set<String> createInstance() throws Exception {
        Set<String> ret = new HashSet<>(getDefaultAdditionalRootClassNames());
        Object value = getValue(ROOT_CLASS_NAMES_BINDING);
        if (value instanceof String) {
            StringBuilder classes = null;
            String v = (String) value;
            String[] rootClasses = v.split(",");
            for (String className : rootClasses) {
                if (className != null && className.trim().length() > 0) {
                    String trimmedClassName = className.trim();
                    ret.add(trimmedClassName);
                    if (classes == null) {
                        classes = new StringBuilder();
                    } else {
                        classes.append(", ");
                    }
                    classes.append(trimmedClassName);
                }
            }
            return Collections.unmodifiableSet(ret);
        } else {
            if (value != null) {
                throw new ServiceDescriptionConfigurationException(
                    String.format("Expected string value for '%s' in the configuration for service '%s'.",
                        ROOT_CLASS_NAMES_BINDING,
                        getServiceDescription().getName()));
            }
        }
        return Collections.unmodifiableSet(ret);
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.defaultAdditionalRootClassNames == null) {
            this.defaultAdditionalRootClassNames = new HashSet<>();
        }
    }
}
