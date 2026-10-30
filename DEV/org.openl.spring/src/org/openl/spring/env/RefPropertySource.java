package org.openl.spring.env;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import org.openl.util.StringUtils;

/**
 * Gives a property the value of another property a {@code $ref} key points at.
 *
 * <p>A property {@code a.b.c} takes its value from {@code x.c} when {@code a.b.$ref = x}. The nearest parent path
 * with a {@code $ref} key wins. The property a reference points at may itself be resolved through a reference, up to
 * two references deep.
 *
 * <p>A reference that leads back to a property already reached through a reference resolves to no value.
 */
// Spring identifies a property source by its name: MutablePropertySources looks sources up and replaces them by name.
@SuppressWarnings("java:S2160")
public class RefPropertySource extends PropertySource<Object> {

    static final String PROPS_NAME = "References to properties";
    static final String REF = ".$ref";
    private static final int MAX_REF_DEPTH = 2;

    private final PropertyResolver resolver;
    private final PropertySources propertySources;

    public RefPropertySource(PropertyResolver resolver, PropertySources propertySources) {
        super(PROPS_NAME);
        this.resolver = resolver;
        this.propertySources = propertySources;
    }

    @Override
    public @Nullable Object getProperty(String name) {
        var resolved = resolve(name, 1, new HashSet<>(), true);
        return resolved == null ? null : resolved.value();
    }

    /**
     * The sources of the {@code $ref} keys the property is resolved through, the nearest first.
     *
     * <p>The value itself is not read, and a placeholder that cannot be resolved does not matter: one in the value of a
     * {@code $ref} key is kept as it is written.
     *
     * @return the sources, or {@code null} when no reference gives the property a value
     */
    @Nullable List<PropertySource<?>> refSourcesOf(String name) {
        var resolved = resolve(name, 1, new HashSet<>(), false);
        return resolved == null ? null : resolved.refSources();
    }

    private @Nullable Resolved resolve(String name, int depth, Set<String> visitedRefs, boolean resolvePlaceholders) {
        if (name.endsWith(REF) || depth > MAX_REF_DEPTH) {
            return null;
        }
        var subName = name;
        var suffix = new StringBuilder();
        var dot = name.length();
        do {
            suffix.insert(0, subName.substring(dot));
            subName = subName.substring(0, dot);
            var ref = findRef(subName + REF, resolvePlaceholders);
            if (ref != null) {
                var targetName = ref.value() + suffix;
                if (!visitedRefs.add(targetName)) {
                    // A property already reached through a reference: the references loop.
                    return null;
                }
                var target = findValue(targetName, depth, visitedRefs, resolvePlaceholders);
                return target == null ? null : target.through(ref.source());
            }
            dot = subName.lastIndexOf('.');

        } while (dot > 0);
        return null;
    }

    /**
     * Finds the {@code $ref} key with a non-blank value. A {@code $ref} key is never resolved through a reference.
     *
     * <p>A placeholder in the value that cannot be resolved fails the lookup, unless the placeholders are not required.
     */
    private @Nullable Ref findRef(String refKey, boolean requirePlaceholders) {
        for (var propertySource : propertySources) {
            var value = propertySource.getProperty(refKey);
            if (value != null) {
                var text = value.toString();
                var refProp = StringUtils.trimToNull(requirePlaceholders
                        ? resolver.resolveRequiredPlaceholders(text)
                        : resolver.resolvePlaceholders(text));
                return refProp == null ? null : new Ref(refProp, propertySource);
            }
        }
        return null;
    }

    private @Nullable Resolved findValue(String name, int depth, Set<String> visitedRefs, boolean resolvePlaceholders) {
        for (var propertySource : propertySources) {
            Resolved resolved;
            if (propertySource == this) {
                resolved = resolve(name, depth + 1, visitedRefs, resolvePlaceholders);
            } else {
                var value = propertySource.getProperty(name);
                resolved = value == null ? null : new Resolved(value.toString(), List.of());
            }
            if (resolved != null) {
                return resolvePlaceholders
                        ? new Resolved(resolver.resolveRequiredPlaceholders(resolved.value()), resolved.refSources())
                        : resolved;
            }
        }
        return null;
    }

    /**
     * A value resolved through references.
     *
     * @param value      the value of the property, its placeholders resolved when asked
     * @param refSources the sources of the {@code $ref} keys the value is resolved through, the nearest first
     */
    record Resolved(String value, List<PropertySource<?>> refSources) {

        private Resolved through(PropertySource<?> refSource) {
            return new Resolved(value, Stream.concat(Stream.of(refSource), refSources.stream()).toList());
        }
    }

    private record Ref(String value, PropertySource<?> source) {
    }
}
