package org.openl.rules.webstudio.web.install;

import org.openl.config.PropertiesHolder;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;

public class DelegatedPropertySource extends PropertySource<PropertiesHolder> {
    DelegatedPropertySource(PropertiesHolder propertiesHolder) {
        super("OpenL temporary properties", propertiesHolder);
    }

    @Override
    public Object getProperty(String name) {
        return getSource().getProperty(name);
    }

    public static PropertyResolver createPropertiesResolver(PropertiesHolder properties) {
        return new AbstractEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                propertySources.addFirst(new DelegatedPropertySource(properties));
            }
        };
    }

}
