package org.openl.rules.repository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openl.rules.repository.api.Repository;
import org.openl.util.ObjectUtils;
import org.openl.util.StringUtils;

/**
 * A factory to create repositories using Java reflection. This instantiator uses the following workflow:
 * <ol>
 * <li>Create a repository instance, using the default constructor</li>
 * <li>Check the instance on implementing {@link Repository} interface</li>
 * <li>Set all parameters using set-methods like Java beans. These methods must apply one String argument. The order of
 * method invocation is undefined. Blank parameters are skipped.</li>
 * <li>Invoke initialize() method</li>
 * <li></li>
 * </ol>
 *
 * @author Yury Molchan
 */
public class RepositoryInstatiator {

    public static Repository newRepository(String prefix, Function<String, String> props) {
        ServiceLoader<RepositoryFactory> factories = ServiceLoader.load(RepositoryFactory.class,
            RepositoryFactory.class.getClassLoader());
        String factoryId = props.apply(prefix + ".factory");
        ArrayList<String> repos = new ArrayList<>();
        for (RepositoryFactory factory : factories) {
            repos.add(factory.getRefID());
            if (factory.accept(factoryId)) {
                return factory.create(key -> {
                    if ("id".equals(key)) {
                        // FIXME: Remove assumption that id is the last part of the prefix.
                        int dot = prefix.lastIndexOf('.');
                        return prefix.substring(dot + 1);
                    }
                    return props.apply(prefix + '.' + key);
                });
            }
        }
        throw new IllegalArgumentException(String.format(
            "Cannot find '%s' repository factory for '%s' configuration. Available repository factories are: %s",
            factoryId,
            prefix,
            repos.stream().collect(Collectors.joining(", "))));
    }

    public static String getRefID(String factoryId) {
        ServiceLoader<RepositoryFactory> factories = ServiceLoader.load(RepositoryFactory.class,
            RepositoryFactory.class.getClassLoader());
        if (factoryId == null) {
            return null;
        }
        for (RepositoryFactory factory : factories) {
            if (factory.accept(factoryId)) {
                return factory.getRefID();
            }
        }
        return null;
    }

    public static void setParams(Object instance, Function<String, String> props) {
        Class<?> clazz = instance.getClass();
        try (Stream<Method> stream = Arrays.stream(clazz.getMethods())) {
            stream.filter(method -> method.getParameterCount() == 1 && method.getName().startsWith("set"))
                .forEach(method -> {
                    String fieldName = method.getName().substring(3);
                    String propertyName = StringUtils.camelToKebab(fieldName);
                    String propertyValue = props.apply(propertyName);
                    boolean propertyExists = StringUtils.isNotBlank(propertyValue);
                    if (propertyExists) {
                        Class<?> type = method.getParameterTypes()[0];
                        Object value = ObjectUtils.convert(propertyValue, type);
                        try {
                            method.invoke(instance, value);
                        } catch (Exception e) {
                            throw new IllegalStateException(
                                String.format("Failed to invoke method '%s.%s(%s)' with value '%s'.",
                                    clazz.getTypeName(),
                                    method.getName(),
                                    type.getSimpleName(),
                                    value),
                                e);
                        }
                    }
                });
        }
    }
}
