package org.openl.rules.repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
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
        String factoryClass = props.apply(prefix + ".factory");
        Repository repository = newInstance(factoryClass);
        setParams(repository, props, prefix);
        initialize(repository);
        return repository;
    }

    private static Repository newInstance(String factory) {
        Object instance;
        try {
            // Instantiate a repository
            Class<?> clazz = Class.forName(factory);
            instance = clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate a repository: " + factory, e);
        } catch (UnsupportedClassVersionError e) {
            throw new IllegalStateException("Library is compiled using newer version of JDK.", e);
        }
        try {
            return (Repository) instance;
        } catch (ClassCastException e) {
            throw new IllegalStateException(String.format("%s must be an implementation of %s.",
                instance.getClass().getTypeName(),
                Repository.class.getTypeName()), e);
        }
    }

    private static void setParams(Object instance, Function<String, String> props, String prefix) {
        Class<?> clazz = instance.getClass();
        try (Stream<Method> stream = Arrays.stream(clazz.getMethods())) {
            stream.filter(method -> method.getParameterCount() == 1 && method.getName().startsWith("set"))
                .forEach(method -> {
                    String fieldName = method.getName().substring(3);
                    String propertyName = prefix + "." + StringUtils.camelToKebab(fieldName);
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

    private static void initialize(Object instance) {
        Class<?> clazz = instance.getClass();
        try {
            // Try to find initialize() method
            Method initMethod = clazz.getMethod("initialize");
            // Execute initialize() method to finish instantiation of the
            // repository.
            initMethod.invoke(instance);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(String.format("Failed on method '%s.initialize()' call.", clazz), e);
        }
    }
}
