package org.openl.rules.ruleservice.storelogdata.db;

import java.util.Arrays;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.openl.rules.ruleservice.storelogdata.PropertiesLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class HibernateSessionFactoryBuilder implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private volatile Properties applicationContextProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private Properties getApplicationContextProperties() {
        if (this.applicationContextProperties == null) {
            synchronized (this) {
                if (this.applicationContextProperties == null) {
                    this.applicationContextProperties = PropertiesLoader
                        .getApplicationContextProperties(applicationContext);
                }
            }
        }
        return this.applicationContextProperties;
    }

    public SessionFactory buildSessionFactory(Class<?>[] entityClasses) {
        Configuration configuration = new Configuration();
        Arrays.stream(entityClasses).forEach(configuration::addAnnotatedClass);
        configuration.setProperties(getApplicationContextProperties());
        StandardServiceRegistryBuilder serviceRegistryBuilder = new StandardServiceRegistryBuilder();
        serviceRegistryBuilder.applySettings(configuration.getProperties());
        StandardServiceRegistry serviceRegistry = serviceRegistryBuilder.build();
        return configuration.buildSessionFactory(serviceRegistry);
    }
}
