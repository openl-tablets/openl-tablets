package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.serialization.DefaultTypingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceDescriptionConfigurationDefaultTypingTypeFactoryBean extends AbstractFactoryBean<DefaultTypingType> {
    private final Logger log = LoggerFactory
        .getLogger(ServiceDescriptionConfigurationDefaultTypingTypeFactoryBean.class);

    private static final String ENABLE_DEFAULT_TYPING = "jacksondatabinding.enableDefaultTyping";
    private static final String DEFAULT_TYPING_TYPE = "jacksondatabinding.defaultTypingType";

    private DefaultTypingType defaultValue = DefaultTypingType.SMART;
    
    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected DefaultTypingType createInstance() {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription != null && serviceDescription.getConfiguration() != null) {
            Object value = serviceDescription.getConfiguration().get(DEFAULT_TYPING_TYPE);
            if (value instanceof String) {
                String v = (String) value;
                if (DefaultTypingType.SMART.name().equalsIgnoreCase(v.trim())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), v.trim());
                    return DefaultTypingType.SMART;
                }
                if (DefaultTypingType.ENABLE.name().equalsIgnoreCase(v.trim())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), v.trim());
                    return DefaultTypingType.ENABLE;
                }
                if (DefaultTypingType.DISABLE.name().equalsIgnoreCase(v.trim())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), v.trim());
                    return DefaultTypingType.DISABLE;
                }
                if (log.isErrorEnabled()) {
                    log.error(
                        "Error in service '{}' configuration. Invalid default typing type is used in '" + DEFAULT_TYPING_TYPE + "'! Default value is used!",
                        serviceDescription.getName());
                }
                return getDefaultValue();
            }
            if (value instanceof DefaultTypingType) {
                log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), ((DefaultTypingType) value).name());
                return (DefaultTypingType) value;
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
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), DefaultTypingType.ENABLE.name());
                    return DefaultTypingType.ENABLE;
                }
                if ("false".equals(((String) value).trim().toLowerCase())) {
                    log.info("Service '{}' uses default typing type '{}'.", serviceDescription.getName(), DefaultTypingType.SMART.name());
                    return DefaultTypingType.SMART;
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
        return DefaultTypingType.class;
    }

    public DefaultTypingType getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(DefaultTypingType defaultValue) {
        this.defaultValue = defaultValue;
    }
}