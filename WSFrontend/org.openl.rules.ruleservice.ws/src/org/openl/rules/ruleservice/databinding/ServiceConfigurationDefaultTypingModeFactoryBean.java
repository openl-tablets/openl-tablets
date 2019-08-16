package org.openl.rules.ruleservice.databinding;

import org.openl.rules.serialization.DefaultTypingMode;

public class ServiceConfigurationDefaultTypingModeFactoryBean extends ServiceConfigurationFactoryBean<DefaultTypingMode> {

    public static final String ENABLE_DEFAULT_TYPING = "jacksondatabinding.enableDefaultTyping";
    public static final String DEFAULT_TYPING_TYPE = "jacksondatabinding.defaultTypingMode";

    @Override
    protected DefaultTypingMode createInstance() throws Exception {
        Object value = getValue(DEFAULT_TYPING_TYPE);
        if (value instanceof String) {
            String v = (String) value;
            if (DefaultTypingMode.SMART.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.SMART;
            }
            if (DefaultTypingMode.ENABLE.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.ENABLE;
            }
            if (DefaultTypingMode.DISABLE.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.DISABLE;
            }
            throw new ServiceConfigurationException(
                String.format("Expected SMART/ENABLE/DISABLE value for '%s' in the configuration for service '%s'.",
                    DEFAULT_TYPING_TYPE,
                    getServiceDescription().getName()));
        }
        if (value instanceof DefaultTypingMode) {
            return (DefaultTypingMode) value;
        }
        if (value != null) {
            throw new ServiceConfigurationException(
                String.format("Expected SMART/ENABLE/DISABLE value for '%s' in the configuration for service '%s'.",
                    DEFAULT_TYPING_TYPE,
                    getServiceDescription().getName()));
        }
        value = getValue(ENABLE_DEFAULT_TYPING);
        if (value instanceof String) {
            if ("true".equalsIgnoreCase(((String) value).trim())) {
                return DefaultTypingMode.ENABLE;
            }
            if ("false".equalsIgnoreCase(((String) value).trim())) {
                return DefaultTypingMode.SMART;
            }
        }
        if (value != null) {
            throw new ServiceConfigurationException(
                String.format("Expected true/false value for '%s' in the configuration for service '%s'.",
                    ENABLE_DEFAULT_TYPING,
                    getServiceDescription().getName()));
        }
        return getDefaultValue();
    }

    @Override
    public Class<?> getObjectType() {
        return DefaultTypingMode.class;
    }
}