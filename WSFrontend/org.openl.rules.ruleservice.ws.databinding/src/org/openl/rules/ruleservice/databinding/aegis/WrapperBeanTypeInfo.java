package org.openl.rules.ruleservice.databinding.aegis;

import java.beans.PropertyDescriptor;
import java.util.List;

import org.apache.cxf.aegis.type.basic.BeanTypeInfo;

/**
 * {@link BeanTypeInfo} with possibility to ignore properties.
 *
 * @author PUdalau
 *
 */
public class WrapperBeanTypeInfo extends BeanTypeInfo {
    private final List<String> ignoredProperties;

    public WrapperBeanTypeInfo(Class<?> typeClass, String defaultNamespace, List<String> ignoredProperties) {
        super(typeClass, defaultNamespace);
        setExtensibleAttributes(false);
        setExtensibleElements(false);
        this.ignoredProperties = ignoredProperties;
    }

    public WrapperBeanTypeInfo(Class<?> typeClass,
            String defaultNamespace,
            boolean initialize,
            List<String> ignoredProperties) {
        super(typeClass, defaultNamespace, initialize);
        this.ignoredProperties = ignoredProperties;
    }

    @Override
    protected void mapProperty(PropertyDescriptor pd) {
        if (pd != null && !ignoredProperties.contains(pd.getName())) {
            super.mapProperty(pd);
        }
    }

    public List<String> getIgnoredProperties() {
        return ignoredProperties;
    }

}
