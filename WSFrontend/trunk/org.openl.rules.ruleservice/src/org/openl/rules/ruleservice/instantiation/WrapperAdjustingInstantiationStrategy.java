package org.openl.rules.ruleservice.instantiation;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WrapperAdjustingInstantiationStrategy extends AClassInstantiationStrategy {
    
    private static final Log log = LogFactory.getLog(WrapperAdjustingInstantiationStrategy.class);

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

        try {
            Field field = clazz.getField("__userHome");
            field.set(null, userHomeFieldValue);
        } catch (Exception e) {
            log.error("failed to set up __userHome", e);
        }

        try {
            Field field = clazz.getField("__src");
            String sourcePath = (String) field.get(null);
            field.set(null, userHomeFieldValue + '/' + sourcePath);
        } catch (Exception e) {
            log.error("failed to set up __src", e);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
