package org.openl.spring.env;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Holds collections of {@link PropertySource}. Add {@link PropertySource} at the beginof the list. Implemented for
 * support Spring 3.2.
 *
 * @author Yury Molchan
 */
class CompositePropertySource extends EnumerablePropertySource<Object> {

    private final LinkedList<PropertySource<?>> propertySources = new LinkedList<>();

    CompositePropertySource(String name) {
        super(name);
    }

    void addFirst(PropertySource<?> propertySource) {
        if (propertySource != null) {
            propertySources.addFirst(propertySource);
        }
    }

    /**
     * No needs to return this wrapper if no or one {@link PropertySource} was added.
     */
    PropertySource<?> get() {
        if (propertySources.isEmpty()) {
            return null;
        }
        if (propertySources.size() == 1) {
            return propertySources.getFirst();
        }
        return this;
    }

    @Override
    public Object getProperty(String name) {
        for (PropertySource<?> propertySource : this.propertySources) {
            Object candidate = propertySource.getProperty(name);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean containsProperty(String name) {
        for (PropertySource<?> propertySource : this.propertySources) {
            if (propertySource.containsProperty(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> names = new LinkedHashSet<>();
        for (PropertySource<?> propertySource : this.propertySources) {
            if (propertySource instanceof EnumerablePropertySource) {
                names.addAll(Arrays.asList(((EnumerablePropertySource<?>) propertySource).getPropertyNames()));
            }
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public String toString() {
        return String
            .format("%s [name='%s', propertySources=%s]", getClass().getSimpleName(), this.name, this.propertySources);
    }

}
