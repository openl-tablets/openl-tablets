package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

public class ServiceDescriptionConfigurationBooleanFactoryBean extends AbstractFactoryBean<Boolean> {
    private final Logger log = LoggerFactory.getLogger(ServiceDescriptionConfigurationBooleanFactoryBean.class);

    private boolean defaultValue;

    private String propertyName;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("protpertyName can't be null!");
        }
        this.propertyName = propertyName;
    }

    public void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected Boolean createInstance() throws Exception {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription != null && serviceDescription.getConfiguration() != null) {
            boolean ret = getDefaultValue();
            if (serviceDescription.getConfiguration() != null) {
                Object value = serviceDescription.getConfiguration().get(getPropertyName().trim());
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
                if (value instanceof String) {
                    if ("true".equals(((String) value).trim().toLowerCase())) {
                        log.info("Service \"{}\" uses " + getPropertyName().trim() + "=TRUE.",
                            serviceDescription.getName());
                        return Boolean.TRUE;
                    }
                    if ("false".equals(((String) value).trim().toLowerCase())) {
                        log.info("Service \"{}\" uses " + getPropertyName().trim() + "=FALSE.",
                            serviceDescription.getName());
                        return Boolean.FALSE;
                    }
                    if (log.isErrorEnabled()) {
                        log.error("Error in service '{}'. Supports only true/false values for " + getPropertyName().trim() + " configuration!", serviceDescription.getName());
                    }
                    return getDefaultValue();
                }else{
                    if (value != null){
                        if (log.isErrorEnabled()) {
                            log.error("Error in service '{}'. Supports only true/false values for " + getPropertyName().trim() + " configuration! Used default value!", serviceDescription.getName());
                        }
                    }
                }
            }
            return ret;
        }

        return getDefaultValue();
    }

    @Override
    public Class<?> getObjectType() {
        return Boolean.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.propertyName, "propertyName must be set!");
    }

}
