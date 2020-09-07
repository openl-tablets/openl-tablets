package org.openl.spring.env;

import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

/**
 * PropertySourcesPropertyResolver implementation
 * allowing to get the property value with an unreplaced placeholder,
 * for internal use only
 *
 * @author ybiruk
 */
class RawPropertyResolver extends PropertySourcesPropertyResolver {

    RawPropertyResolver(PropertySources propertySources) {
        super(propertySources);
    }

    String getRawProperty(String key) {
        return getPropertyAsRawString(key);
    }

}
