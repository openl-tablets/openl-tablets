package org.openl.rules.ruleservice.instantiation;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WrapperAdjustingInstantiationStrategy extends RulesInstantiationStrategy {  
    private static final Log LOG = LogFactory.getLog(WrapperAdjustingInstantiationStrategy.class);

    private final String userHomeFieldValue;

    public WrapperAdjustingInstantiationStrategy(String userHomeFieldValue, Class<?> clazz) {
        super(clazz);
        this.userHomeFieldValue = userHomeFieldValue;
    }
    
    public WrapperAdjustingInstantiationStrategy(String userHomeFieldValue, String className, ClassLoader loader) {
        super(className, loader);
        this.userHomeFieldValue = userHomeFieldValue;
    }

    @Override
    protected Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        adjustToServerSettings(clazz);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        try {
            return clazz.newInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void adjustToServerSettings(Class<?> clazz) {
        try {
            Field field = clazz.getField("__userHome");
            field.set(null, userHomeFieldValue);
        } catch (Exception e) {
            LOG.error("Failed to set up __userHome", e);
        }

        try {
            Field field = clazz.getField("__src");
            String sourcePath = (String) field.get(null);
            field.set(null, userHomeFieldValue + '/' + sourcePath);
        } catch (Exception e) {
            LOG.error("Failed to set up __src", e);
        }
    }
}
