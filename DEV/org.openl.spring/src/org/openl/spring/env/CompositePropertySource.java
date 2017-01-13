package org.openl.spring.env;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Holds collections of {@link PropertySource}. Add {@link PropertySource} at
 * the beginof the list. Implemented for support Spring 3.2.
 * 
 * @author Yury Molchan
 */
class CompositePropertySource extends EnumerablePropertySource<Object> {

    private final LinkedList<PropertySource<?>> propertySources = new LinkedList<PropertySource<?>>();

    CompositePropertySource(String name) {
        super(name);
    }

    /**
     * The next source should override the previous.
     */
    void addFirst(ResourcePatternResolver resourcePattern, String location) {
        if (location == null) {
            return;
        }
        if (location.matches("[\\{\\}]")) {
            log.info("! Unresolved: '{}'", location);
        }
        Resource[] resources;
        try {
            resources = resourcePattern.getResources(location);
        } catch (IOException e) {
            debug("!      Error: '{}'", location, e);
            return;
        }
        if (CollectionUtils.isEmpty(resources)) {
            debug("-  Not found: '{}'", location);
            return;
        }
        CompositePropertySource propertySource = new CompositePropertySource(location);
        for (Resource resource : resources) {
            try {
                if (resource.exists()) {
                    propertySource.addFirst(new ResourcePropertySource(resource));
                    log.info("+        Add: [{}] '{}'", location, getInfo(resource));
                } else {
                    debug("-  Not exist: [{}] '{}'", location, getInfo(resource));
                }
            } catch (Exception ex) {
                debug("!      Error: [{}] '{}'", location, getInfo(resource), ex);
            }
        }
        addFirst(propertySource.get());
    }

    private void addFirst(PropertySource<?> propertySource) {
        if (propertySource != null) {
            propertySources.addFirst(propertySource);
        }
    }

    /**
     * No needs to return this wrapper if no or one {@link PropertySource} was
     * added.
     */
    private PropertySource<?> get() {
        if (propertySources.isEmpty()) {
            return null;
        }
        if (propertySources.size() == 1) {
            return propertySources.getFirst();
        }
        return this;
    }

    private Object getInfo(Resource resource) {
        try {
            return resource.getURL();
        } catch (Exception e) {
            return resource;
        }
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
        Set<String> names = new LinkedHashSet<String>();
        for (PropertySource<?> propertySource : this.propertySources) {
            if (propertySource instanceof EnumerablePropertySource) {
                names.addAll(Arrays.asList(((EnumerablePropertySource<?>) propertySource).getPropertyNames()));
            }
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public String toString() {
        return String.format("%s [name='%s', propertySources=%s]",
            getClass().getSimpleName(),
            this.name,
            this.propertySources);
    }

    private final Logger log = LoggerFactory.getLogger(CompositePropertySource.class);

    boolean debug;

    private void debug(String message, Object... resource) {
        if (debug) {
            log.info(message, resource);
        } else {
            log.debug(message, resource);
        }
    }
}
