package org.openl.rules.ruleservice.databinding.annotation;

public final class JacksonBindingConfigurationUtils {

    private JacksonBindingConfigurationUtils() {
    }

    @SuppressWarnings("unchecked")
    public static boolean isConfiguration(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return clazz.isAnnotationPresent(MixInClass.class);
    }
}
