package org.openl.rules.ruleservice.databinding;

import java.util.Objects;

import org.springframework.util.Assert;

public class ServiceConfigurationBooleanFactoryBean extends ServiceConfigurationFactoryBean<Boolean> {

    private String propertyName;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = Objects.requireNonNull(propertyName, "propertyName cannot be null");
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
            throw new ServiceConfigurationException(
                String.format("Expected true/false value for '%s' in the deployment configuration for service '%s'.",
                    getPropertyName().trim(),
                    getServiceDescription().getName()));
        } else {
            if (value != null) {
                throw new ServiceConfigurationException(String.format(
                    "Expected true/false value for '%s' in the deployment configuration for service '%s'.",
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
    public void afterPropertiesSet() {
        Assert.notNull(this.propertyName, "propertyName cannot be null");
    }

}
