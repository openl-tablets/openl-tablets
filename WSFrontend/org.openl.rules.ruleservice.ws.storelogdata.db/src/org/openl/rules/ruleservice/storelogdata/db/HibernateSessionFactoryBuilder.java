package org.openl.rules.ruleservice.storelogdata.db;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Setter;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.openl.rules.ruleservice.storelogdata.PropertiesLoader;

public class HibernateSessionFactoryBuilder implements ApplicationContextAware {

    @Setter
    private ApplicationContext applicationContext;
    private final AtomicReference<Properties> applicationContextProperties = new AtomicReference<>();

    private Properties getApplicationContextProperties() {
        var properties = applicationContextProperties.get();
        if (properties == null) {
            synchronized (this) {
                properties = applicationContextProperties.get();
                if (properties == null) {
                    properties = PropertiesLoader.getApplicationContextProperties(applicationContext);
                    applicationContextProperties.set(properties);
                }
            }
        }
        return properties;
    }

    public SessionFactory buildSessionFactory(Class<?>[] entityClasses) {
        var configuration = new Configuration();
        Arrays.stream(entityClasses).forEach(configuration::addAnnotatedClass);
        configuration.setProperties(getApplicationContextProperties());
        var serviceRegistryBuilder = new StandardServiceRegistryBuilder();
        serviceRegistryBuilder.applySettings(configuration.getProperties());
        var serviceRegistry = serviceRegistryBuilder.build();
        return configuration.buildSessionFactory(serviceRegistry);
    }
}
