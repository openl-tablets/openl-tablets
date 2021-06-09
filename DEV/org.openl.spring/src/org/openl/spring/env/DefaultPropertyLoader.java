package org.openl.spring.env;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

/**
 * Loads OpenL default properties from <code>classpath*:openl-default.properties</code>
 *
 * @author Yury Molchan
 */
public class DefaultPropertyLoader implements ApplicationContextInitializer<ConfigurableApplicationContext>, BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        initialize(environment);
    }

    private Environment environment;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        initialize((ConfigurableEnvironment) environment);
    }

    private void initialize(ConfigurableEnvironment environment) {
        MutablePropertySources propertySources = environment.getPropertySources();
        if (!propertySources.contains(DefaultPropertySource.PROPS_NAME)) {
            ConfigLog.LOG.info("Loading default properties...");
            propertySources.addLast(new DefaultPropertySource());
            ConfigLog.LOG.info("Register reference property processor...");
            propertySources.addLast(new RefPropertySource(propertySources));
            ConfigLog.LOG.info("Loading OpenL System Info properties...");
            propertySources.addFirst(new SysInfoPropertySource());
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}