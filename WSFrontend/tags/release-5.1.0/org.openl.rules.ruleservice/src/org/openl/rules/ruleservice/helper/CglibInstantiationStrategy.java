package org.openl.rules.ruleservice.helper;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.Enhancer;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CglibInstantiationStrategy implements InstantiationStrategy {
    private final String userHomeFieldValue;

    public CglibInstantiationStrategy(String userHomeFieldValue) {
        this.userHomeFieldValue = userHomeFieldValue;
    }

    private static final Log log = LogFactory.getLog(CglibInstantiationStrategy.class);

    public Object instantiate(Class<?> clazz) {
        Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(new NewInstanceInterceptor(clazz));

        try {
            Field field = clazz.getField("__userHome");
            field.set(null, userHomeFieldValue);
        } catch (Exception e) {
            log.error("failed to set up __userHome", e);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        try {
            return enhancer.create();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private static class NewInstanceInterceptor extends SimpleInstantiationStrategy implements MethodInterceptor {
		final Class<?> clazz;

		private NewInstanceInterceptor(Class<?> clazz) {
			this.clazz = clazz;
		}

		public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
            Object o = instantiate(clazz);
            return mp.invoke(o, args);
        }
	}
}
