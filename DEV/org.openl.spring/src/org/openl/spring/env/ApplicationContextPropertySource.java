package org.openl.spring.env;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;

/**
 * Resolves properties as beans from a spring application context.
 *
 * @author Yury Molchan
 */
public class ApplicationContextPropertySource extends PropertySource<ConfigurableApplicationContext> {

    public static final String PROPS_NAME = "Spring Application Context beans";

    public ApplicationContextPropertySource(ConfigurableApplicationContext source) {
        super(PROPS_NAME, source);
    }

    @Override
    public Object getProperty(String name) {
        try {
            return name.startsWith("spring.") && source.isActive() ? source.getBean(name.substring(7)) : null;
        } catch (BeansException ignore) {
            return null;
        }
    }
}
