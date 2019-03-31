package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

public class ServiceDescriptionConfigurationStringFactoryBean extends AbstractFactoryBean<String> {
    private final Logger log = LoggerFactory.getLogger(ServiceDescriptionConfigurationBooleanFactoryBean.class);

    private String defaultValue;

    private String propertyName;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("protpertyName must not be null!");
        }
        this.propertyName = propertyName;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected String createInstance() throws Exception {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription != null && serviceDescription.getConfiguration() != null) {
            String ret = getDefaultValue();
            if (serviceDescription.getConfiguration() != null) {
                Object value = serviceDescription.getConfiguration().get(getPropertyName().trim());
                if (value instanceof String) {
                    return (String) value;
                } else {
                    if (value != null && log.isErrorEnabled()) {
                        log.error("Error in service '{}'. Supports only string values for " + getPropertyName()
                            .trim() + " configuration! Default value has been used!");
                    }
                }
            }
            return ret;
        }

        return getDefaultValue();
    }

    @Override
    public Class<?> getObjectType() {
        return String.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.propertyName, "propertyName must be set!");
    }

}
