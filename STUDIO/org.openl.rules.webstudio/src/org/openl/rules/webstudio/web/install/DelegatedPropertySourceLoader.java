package org.openl.rules.webstudio.web.install;

import java.util.Iterator;
import java.util.Properties;

import org.openl.config.PropertiesHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.StringValueResolver;

public class DelegatedPropertySourceLoader extends PlaceholderConfigurerSupport
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private final PropertiesHolder propertiesHolder;

    DelegatedPropertySourceLoader(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    @Override
    public void initialize(ConfigurableApplicationContext appContext) {
        ConfigurableEnvironment env = appContext.getEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();

        loadProperties(propertySources);
    }

    private void loadProperties(MutablePropertySources propertySources) {
        Iterator<PropertySource<?>> iterator = propertySources.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        propertySources.addFirst(new DelegatedPropertySource(propertiesHolder));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        MutablePropertySources propertySources = new MutablePropertySources();
        loadProperties(propertySources);
        processProperties(beanFactory, propertySources);
    }

    protected void processProperties(ConfigurableListableBeanFactory beanFactory,
        PropertySources propertySources) throws BeansException {
        final PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
        propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
        propertyResolver.setValueSeparator(this.valueSeparator);

        StringValueResolver valueResolver = strVal -> {
            String resolved = ignoreUnresolvablePlaceholders ? propertyResolver.resolvePlaceholders(strVal)
                                                             : propertyResolver.resolveRequiredPlaceholders(strVal);
            return resolved.equals(nullValue) ? null : resolved;
        };

        doProcessProperties(beanFactory, valueResolver);
    }

    @Deprecated
    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) throws
                                                                                                    BeansException {
        throw new UnsupportedOperationException(
            "Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");

    }
}
