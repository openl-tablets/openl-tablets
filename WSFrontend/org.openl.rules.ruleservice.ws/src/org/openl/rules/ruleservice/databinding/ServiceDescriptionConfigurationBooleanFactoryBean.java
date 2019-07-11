package org.openl.rules.ruleservice.databinding;

import org.springframework.util.Assert;

public class ServiceDescriptionConfigurationBooleanFactoryBean extends ServiceDescriptionConfigurationFactoryBean<Boolean> {

    private String propertyName;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("property name can't be null!");
        }
        this.propertyName = propertyName;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected Boolean createInstance() throws Exception {
        boolean ret = getDefaultValue();
        Object value = getValue(getPropertyName().trim());
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            if ("true".equalsIgnoreCase(((String) value).trim())) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(((String) value).trim())) {
                return Boolean.FALSE;
            }
            throw new ServiceDescriptionConfigurationException(
                String.format("Expected true/false value for '%s' in the configuration for service '%s'!",
                    getPropertyName().trim(),
                    getServiceDescription().getName()));
        } else {
            if (value != null) {
                throw new ServiceDescriptionConfigurationException(
                    String.format("Expected true/false value for '%s' in the configuration for service '%s'!",
                        getPropertyName().trim(),
                        getServiceDescription().getName()));
            }
        }
        return ret;
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
