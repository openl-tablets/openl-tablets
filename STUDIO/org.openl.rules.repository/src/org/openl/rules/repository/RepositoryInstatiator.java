package org.openl.rules.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class RepositoryInstatiator {

    /**
     * The repository factories declared on the class path, looked up once.
     *
     * <p>The answer cannot change while the class loader holding them lives, so every question about a
     * repository type is answered from the same factories.
     *
     * <p>A factory that cannot be created is left out, and the repository types the remaining ones serve
     * keep working.
     */
    private static final List<RepositoryFactory> FACTORIES = loadFactories();

    private static List<RepositoryFactory> loadFactories() {
        var factories = new ArrayList<RepositoryFactory>();
        var providers = ServiceLoader.load(RepositoryFactory.class, RepositoryFactory.class.getClassLoader())
                .stream()
                .toList();
        for (var provider : providers) {
            try {
                factories.add(provider.get());
            } catch (ServiceConfigurationError e) {
                log.warn("Repository factory '{}' cannot be created, so it is skipped.",
                        provider.type().getName(),
                        e);
            }
        }
        return List.copyOf(factories);
    }

    public static Repository newRepository(String prefix, Function<String, String> props) {
        var factoryId = props.apply(prefix + ".factory");
        if (Objects.isNull(factoryId)) {
            throw new IllegalArgumentException(
                    "Invalid configuration for repository %s".formatted(
                            prefix.substring(prefix.indexOf(".") + 1)
                    )
            );
        }
        var repos = new ArrayList<String>();
        for (RepositoryFactory factory : FACTORIES) {
            repos.add(factory.getRefID());
            if (factory.accept(factoryId)) {
                return new PathCheckedRepository(factory.create(key -> {
                    if ("id".equals(key)) {
                        // FIXME: Remove assumption that id is the last part of the prefix.
                        var dot = prefix.lastIndexOf('.');
                        return prefix.substring(dot + 1);
                    }
                    var value = props.apply(prefix + '.' + key);
                    if (Objects.isNull(value)) {
                        // If not found the property with the specific prefix, then use defaults for the factory.
                        value = props.apply(factory.getRefID() + '.' + key);
                    }
                    return value;
                }));
            }
        }
        throw new IllegalArgumentException("Failed to find '%s' repository factory for '%s' configuration. Available repository factories are: %s".formatted(
                factoryId,
                prefix, String.join(", ", repos)));
    }

    public static String getRefID(String factoryId) {
        if (factoryId == null) {
            return null;
        }
        for (RepositoryFactory factory : FACTORIES) {
            if (factory.accept(factoryId)) {
                return factory.getRefID();
            }
        }
        return null;
    }

    public static void setParams(Object instance, Function<String, String> props) {
        Class<?> clazz = instance.getClass();
        try (var stream = Arrays.stream(clazz.getMethods())) {
            stream.filter(method -> method.getParameterCount() == 1 && method.getName().startsWith("set"))
                    .forEach(method -> {
                        var fieldName = method.getName().substring(3);
                        String propertyName = StringUtils.camelToKebab(fieldName);
                        var propertyValue = props.apply(propertyName);
                        var propertyExists = StringUtils.isNotBlank(propertyValue);
                        if (propertyExists) {
                            Class<?> type = method.getParameterTypes()[0];
                            Object value = ObjectUtils.convert(propertyValue, type);
                            try {
                                method.invoke(instance, value);
                            } catch (Exception e) {
                                throw new IllegalStateException(
                                        "Failed to invoke method '%s.%s(%s)' with value '%s'.".formatted(
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
