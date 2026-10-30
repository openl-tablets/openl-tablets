package org.openl.spring.env;

import java.util.prefs.Preferences;

import org.jspecify.annotations.Nullable;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

/**
 * Tells whether a property is set outside the application settings, so the application cannot change it.
 *
 * <p>Asked for {@code _sys_disable_<name>}, it answers {@code true} when the value of {@code <name>} comes from a
 * property source placed before this one: system properties, environment variables, JNDI or the servlet context.
 *
 * <p>A value resolved through {@code $ref} counts as set outside when a {@code $ref} key on its path comes from such a
 * source. The value the reference points at does not count: set there, it is a shared default a property may
 * override. A property set directly in the application settings is never resolved through a reference.
 */
// Spring identifies a property source by its name: MutablePropertySources looks sources up and replaces them by name.
@SuppressWarnings("java:S2160")
public class DisablePropertySource extends PropertySource<Preferences> {

    public static final String PROPS_NAME = "Disable properties";
    public static final String PROPS_PREFIX = "_sys_disable_";

    private final PropertySources propertySources;

    DisablePropertySource(PropertySources propertySources) {
        super(PROPS_NAME);
        this.propertySources = propertySources;
    }

    @Override
    public @Nullable Object getProperty(String name) {
        if (name.startsWith(PROPS_PREFIX)) {
            return String.valueOf(isDisabled(name.substring(PROPS_PREFIX.length())));
        }
        return null;
    }

    private boolean isDisabled(String name) {
        var outerSources = propertySources.stream().takeWhile(source -> source != this).toList();
        for (var propertySource : propertySources) {
            if (propertySource instanceof RefPropertySource references) {
                var refSources = references.refSourcesOf(name);
                return refSources != null && refSources.stream().anyMatch(outerSources::contains);
            }
            if (propertySource.getProperty(name) != null) {
                return outerSources.contains(propertySource);
            }
        }
        return false;
    }
}
