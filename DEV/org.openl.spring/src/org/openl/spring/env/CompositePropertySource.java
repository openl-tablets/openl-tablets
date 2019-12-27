package org.openl.spring.env;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.openl.util.CollectionUtils;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Holds collections of {@link PropertySource}. Add {@link PropertySource} at the begin of the list.
 *
 * @author Yury Molchan
 */
class CompositePropertySource extends EnumerablePropertySource<Deque<PropertySource<?>>> {

    CompositePropertySource(String name) {
        super(name, new LinkedList<>());
    }

    /**
     * The next source should override the previous.
     */
    void addLocation(String location) {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(location);
        } catch (IOException e) {
            ConfigLog.LOG.debug("!     Error: '{}'", new Object[] { location, e });
            return;
        }
        if (CollectionUtils.isEmpty(resources)) {
            ConfigLog.LOG.debug("- Not found: [{}]", location);
            return;
        }

        Arrays.sort(resources,
            Comparator.comparing(Resource::getFilename,
                Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())));
        for (Resource resource : resources) {
            try {
                if (resource.exists()) {
                    PropertySource<?> propertySource = new ResourcePropertySource(resource);
                    getSource().addFirst(propertySource);
                    ConfigLog.LOG.info("+        Add: [{}] '{}'", location, getInfo(resource));
                } else {
                    ConfigLog.LOG.debug("- Not exist: [{}] '{}'", location, getInfo(resource));
                }
            } catch (Exception ex) {
                ConfigLog.LOG.debug("!     Error: [{}] '{}'", location, getInfo(resource), ex);
            }
        }
        return;
    }

    private static Object getInfo(Resource resource) {
        try {
            return resource.getURL();
        } catch (Exception e) {
            return resource;
        }
    }

    @Override
    public Object getProperty(String name) {
        for (PropertySource<?> propertySource : source) {
            Object candidate = propertySource.getProperty(name);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean containsProperty(String name) {
        for (PropertySource<?> propertySource : source) {
            if (propertySource.containsProperty(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> names = new LinkedHashSet<>();
        for (PropertySource<?> propertySource : source) {
            if (propertySource instanceof EnumerablePropertySource) {
                names.addAll(Arrays.asList(((EnumerablePropertySource<?>) propertySource).getPropertyNames()));
            }
        }
        return names.toArray(new String[0]);
    }
}
