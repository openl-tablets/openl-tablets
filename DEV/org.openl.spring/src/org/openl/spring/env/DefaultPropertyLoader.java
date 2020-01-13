package org.openl.spring.env;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Replacement of {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer}. Allows to load OpenL
 * default properties from <code>classpath*:openl-default.properties</code>
 *
 * @author Yury Molchan
 * @see org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * @see PlaceholderConfigurerSupport
 */
public class DefaultPropertyLoader extends PropertySourcesPlaceholderConfigurer {

    {
        setIgnoreResourceNotFound(true);
    }

    @Nullable
    private MutablePropertySources propertySources;

    @Nullable
    private Environment environment;

    @Nullable
    private PropertySources appliedPropertySources;

    @Override
    public void setPropertySources(PropertySources propertySources) {
        this.propertySources = new MutablePropertySources(propertySources);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (this.propertySources == null) {
            this.propertySources = new MutablePropertySources();
            if (this.environment != null) {
                this.propertySources.addLast(
                    new PropertySource<Environment>(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, this.environment) {
                        @Override
                        @Nullable
                        public String getProperty(String key) {
                            return this.source.getProperty(key);
                        }
                    });
            }
            try {
                PropertySource<?> localPropertySource = new PropertiesPropertySource(
                    LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME,
                    mergeProperties());
                if (this.localOverride) {
                    this.propertySources.addFirst(localPropertySource);
                } else {
                    this.propertySources.addLast(localPropertySource);
                }
            } catch (IOException ex) {
                throw new BeanInitializationException("Could not load properties", ex);
            }
            if (!new PropertySourcesPropertyResolver(this.propertySources)
                .containsProperty(DefaultPropertySource.OPENL_CONFIG_LOADED)) {
                propertySources.addLast(new DefaultPropertySource());
            }
        }

        processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
        this.appliedPropertySources = this.propertySources;
    }

    @Override
    public PropertySources getAppliedPropertySources() throws IllegalStateException {
        Assert.state(this.appliedPropertySources != null, "PropertySources have not yet been applied");
        return this.appliedPropertySources;
    }
}
