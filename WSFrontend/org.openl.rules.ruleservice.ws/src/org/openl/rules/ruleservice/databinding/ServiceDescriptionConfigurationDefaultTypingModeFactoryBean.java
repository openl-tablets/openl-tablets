package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.serialization.DefaultTypingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceDescriptionConfigurationDefaultTypingModeFactoryBean extends AbstractFactoryBean<DefaultTypingMode> {
    private final Logger log = LoggerFactory
        .getLogger(ServiceDescriptionConfigurationDefaultTypingModeFactoryBean.class);

    private static final String ENABLE_DEFAULT_TYPING = "jacksondatabinding.enableDefaultTyping";
    private static final String DEFAULT_TYPING_TYPE = "jacksondatabinding.defaultTypingMode";

    private DefaultTypingMode defaultValue = DefaultTypingMode.SMART;
    
    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected DefaultTypingMode createInstance() {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription != null && serviceDescription.getConfiguration() != null) {
            Object value = serviceDescription.getConfiguration().get(DEFAULT_TYPING_TYPE);
            if (value instanceof String) {
                String v = (String) value;
                if (DefaultTypingMode.SMART.name().equalsIgnoreCase(v.trim())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), v.trim());
                    return DefaultTypingMode.SMART;
                }
                if (DefaultTypingMode.ENABLE.name().equalsIgnoreCase(v.trim())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), v.trim());
                    return DefaultTypingMode.ENABLE;
                }
                if (DefaultTypingMode.DISABLE.name().equalsIgnoreCase(v.trim())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), v.trim());
                    return DefaultTypingMode.DISABLE;
                }
                if (log.isErrorEnabled()) {
                    log.error(
                        "Error in service '{}' configuration. Invalid default typing type is used in '" + DEFAULT_TYPING_TYPE + "'! Default value is used!",
                        serviceDescription.getName());
                }
                return getDefaultValue();
            }
            if (value instanceof DefaultTypingMode) {
                log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), ((DefaultTypingMode) value).name());
                return (DefaultTypingMode) value;
            }
            if (value != null) {
                if (log.isErrorEnabled()) {
                    log.error(
                        "Error in service '{}' configuration. Unsupported value is used in '" + DEFAULT_TYPING_TYPE + "'! Default value is used!",
                        serviceDescription.getName());
                }
                return getDefaultValue();
            }
            value = serviceDescription.getConfiguration().get(ENABLE_DEFAULT_TYPING);
            if (value instanceof String) {
                if ("true".equals(((String) value).trim().toLowerCase())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), DefaultTypingMode.ENABLE.name());
                    return DefaultTypingMode.ENABLE;
                }
                if ("false".equals(((String) value).trim().toLowerCase())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), DefaultTypingMode.SMART.name());
                    return DefaultTypingMode.SMART;
                }
            }
            if (value != null) {
                if (log.isErrorEnabled()) {
                    log.error(
                        "Error in service '{}' configuration. Unsupported value is used in '" + ENABLE_DEFAULT_TYPING + "'! Default value is used!",
                        serviceDescription.getName());
                }
                return getDefaultValue();
            }
        }
        return getDefaultValue();
    }

    @Override
    public Class<?> getObjectType() {
        return DefaultTypingMode.class;
    }

    public DefaultTypingMode getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(DefaultTypingMode defaultValue) {
        this.defaultValue = defaultValue;
    }
}