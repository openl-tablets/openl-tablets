package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ServiceDescriptionConfigurationRootClassNamesBindingFactoryBean extends AbstractFactoryBean<Set<String>> {
    private final Logger log = LoggerFactory
        .getLogger(ServiceDescriptionConfigurationRootClassNamesBindingFactoryBean.class);

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
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected Set<String> createInstance() throws Exception {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription != null && serviceDescription.getConfiguration() != null) {
            Set<String> ret = new HashSet<>(getDefaultAdditionalRootClassNames());
            if (serviceDescription.getConfiguration() != null) {
                Object value = serviceDescription.getConfiguration().get(ROOT_CLASS_NAMES_BINDING);
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
                    log.info("Service '{}' uses root class names for binding. Classes: {}",
                        serviceDescription.getName(),
                        classes);
                    return Collections.unmodifiableSet(ret);
                } else {
                    if (value != null && log.isErrorEnabled()) {
                        log.error(
                            "Error in service '{}' configuration. Unsupported value is used in '" + ROOT_CLASS_NAMES_BINDING + "'! Default value is used!",
                            serviceDescription.getName());
                    }
                }
            }
            return ret;
        }

        return Collections.unmodifiableSet(getDefaultAdditionalRootClassNames());
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
