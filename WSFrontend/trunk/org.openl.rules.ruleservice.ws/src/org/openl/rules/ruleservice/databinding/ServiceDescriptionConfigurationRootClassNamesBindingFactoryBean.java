package org.openl.rules.ruleservice.databinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceDescriptionConfigurationRootClassNamesBindingFactoryBean extends AbstractFactoryBean<Set<String>> {
    private final Log log = LogFactory.getLog(ServiceDescriptionConfigurationRootClassNamesBindingFactoryBean.class);

    private static final String ROOT_CLASS_NAMES_BINDING = "rootClassNamesBinding";

    private Set<String> additionalRootClassNames;

    public void setAdditionalRootClassNames(Set<String> additionalRootClassNames) {
        if (additionalRootClassNames == null) {
            throw new IllegalArgumentException("addtionalRootClassNames arg can't be null");
        }
        this.additionalRootClassNames = additionalRootClassNames;
    }

    public Set<String> getAdditionalRootClassNames() {
        return additionalRootClassNames;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected Set<String> createInstance() throws Exception {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription != null && serviceDescription.getConfiguration() != null) {
            Set<String> ret = new HashSet<String>(getAdditionalRootClassNames());
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
                    if (log.isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Service \"");
                        sb.append(serviceDescription.getName());
                        sb.append("\" uses root class names for binding. Classes: ");
                        sb.append(classes);
                        log.info(sb.toString());
                    }
                    return Collections.unmodifiableSet(ret);
                }
            }
            return ret;
        }

        return Collections.unmodifiableSet(getAdditionalRootClassNames());
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.additionalRootClassNames == null) {
            this.additionalRootClassNames = new HashSet<String>();
        }
    }
}
