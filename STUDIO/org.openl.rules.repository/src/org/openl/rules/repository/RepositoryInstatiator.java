package org.openl.rules.repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.repository.api.Repository;
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

    /**
     * Create new repository instance.
     *
     * @param factory the class name to instantiate.
     * @param params the initialization parameters.
     * @return the initialized repository.
     */
    public static Repository newRepository(String factory, Map<String, String> params) {
        Repository repository = newInstance(factory);
        if (params != null) {
            setParams(repository, params);
        }
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
            throw new IllegalStateException("Library was compiled using newer version of JDK", e);
        }
        try {
            return (Repository) instance;
        } catch (ClassCastException e) {
            throw new IllegalStateException(instance.getClass() + " must implement " + Repository.class, e);
        }
    }

    private static void setParams(Object instance, Map<String, String> params) {
        Class<?> clazz = instance.getClass();
        for (Map.Entry<String, String> param : params.entrySet()) {
            String value = param.getValue();
            if (StringUtils.isNotBlank(value)) {
                String name = param.getKey();
                String setter = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                try {
                    Method setMethod = clazz.getMethod(setter, String.class);
                    setMethod.invoke(instance, value);
                } catch (NoSuchMethodException e) {
                    Method[] methods = clazz.getMethods();
                    for (Method method : methods) {
                        if (method.getName().equals(setter)) {
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length == 1) {
                                try {
                                    method.invoke(instance, convert(parameterTypes[0], value));
                                    // Found needed setter
                                    break;
                                } catch (NoSuchMethodException | IllegalAccessException ignore) {
                                    // Can't convert using this method. Skip.
                                } catch (InvocationTargetException e1) {
                                    // The underlying method throws an exception
                                    throw new IllegalStateException(
                                        "Failed to invoke " + setter + "(" + parameterTypes[0]
                                            .getSimpleName() + ") method in: " + clazz,
                                        e1);
                                }
                            }
                        }
                    }
                    // Didn't find setter, skip this param. For example not always exists setUri(String).
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to invoke " + setter + "(String) method in: " + clazz, e);
                }
            }
        }
    }

    private static Object convert(Class<?> parameterType,
            String value) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method valueOfMethod = parameterType.getMethod("valueOf", String.class);
        return valueOfMethod.invoke(null, value);
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
            throw new IllegalStateException("Failed to call initialize() in: " + clazz, e);
        }
    }
}
