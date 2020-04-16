package org.openl.rules.ruleservice.databinding;

import org.openl.rules.serialization.DefaultTypingMode;

public class ServiceConfigurationDefaultTypingModeFactoryBean extends ServiceConfigurationFactoryBean<DefaultTypingMode> {

    public static final String DEFAULT_TYPING_TYPE = "jackson.defaultTypingMode";

    @Override
    protected DefaultTypingMode createInstance() throws Exception {
        Object value = getValue(DEFAULT_TYPING_TYPE);
        if (value instanceof String) {
            String v = (String) value;
            if (DefaultTypingMode.DISABLED.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.DISABLED;
            } else if (DefaultTypingMode.OBJECT_AND_NON_CONCRETE.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.OBJECT_AND_NON_CONCRETE;
            } else if (DefaultTypingMode.EVERYTHING.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.EVERYTHING;
            } else if (DefaultTypingMode.NON_CONCRETE_AND_ARRAYS.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.NON_CONCRETE_AND_ARRAYS;
            } else if (DefaultTypingMode.JAVA_LANG_OBJECT.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.JAVA_LANG_OBJECT;
            } else if (DefaultTypingMode.NON_FINAL.name().equalsIgnoreCase(v.trim())) {
                return DefaultTypingMode.NON_FINAL;
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