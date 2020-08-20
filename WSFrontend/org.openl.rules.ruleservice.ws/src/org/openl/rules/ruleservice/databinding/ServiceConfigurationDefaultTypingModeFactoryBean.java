package org.openl.rules.ruleservice.databinding;

import org.openl.rules.serialization.DefaultTypingMode;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBeanHelper;

public class ServiceConfigurationDefaultTypingModeFactoryBean extends ServiceConfigurationFactoryBean<DefaultTypingMode> {

    public static final String DEFAULT_TYPING_TYPE = "jackson.defaultTypingMode";

    @Override
    protected DefaultTypingMode createInstance() throws Exception {
        Object value = getValue(DEFAULT_TYPING_TYPE);
        if (value instanceof String) {
            String v = (String) value;
            DefaultTypingMode defaultTypingMode = JacksonObjectMapperFactoryBeanHelper.toDefaultTypingMode(v);
            if (defaultTypingMode != null) {
                return defaultTypingMode;
            }
            throw new ServiceConfigurationException(String.format(
                "Expected JAVA_LANG_OBJECT/OBJECT_AND_NON_CONCRETE/NON_CONCRETE_AND_ARRAYS/NON_FINAL/EVERYTHING/DISABLED value for '%s' in the configuration for service '%s'.",
                DEFAULT_TYPING_TYPE,
                getServiceDescription().getName()));
        }
        if (value instanceof DefaultTypingMode) {
            return (DefaultTypingMode) value;
        }
        if (value != null) {
            throw new ServiceConfigurationException(String.format(
                "Expected JAVA_LANG_OBJECT/OBJECT_AND_NON_CONCRETE/NON_CONCRETE_AND_ARRAYS/NON_FINAL/EVERYTHING/DISABLED; value for '%s' in the configuration for service '%s'.",
                DEFAULT_TYPING_TYPE,
                getServiceDescription().getName()));
        }
        return getDefaultValue();
    }

    @Override
    public Class<?> getObjectType() {
        return DefaultTypingMode.class;
    }
}