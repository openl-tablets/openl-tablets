package org.openl.rules.repository;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.repository.api.Repository;
import org.openl.util.StringUtils;

/**
 * A factory to create repositories using Java reflection. This instantiator
 * uses the following workflow:
 * <ol>
 * <li>Create a repository instance, using the default constructor</li>
 * <li>Check the instance on implementing {@link Repository} interface</li>
 * <li>Set all parameters using set-methods like Java beans. These methods must
 * apply one String argument. The order of method invocation is undefined. Blank
 * parameters are skipped.</li>
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
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to invoke " + setter + "(String) method in: " + clazz, e);
                }
            }
        }
    }

    private static void initialize(Object instance) {
        Class<?> clazz = instance.getClass();
        Method initMethod;
        try {
            // Try to find initialize() method
            initMethod = clazz.getMethod("initialize");
        } catch (NoSuchMethodException e) {
            initMethod = null;
        }
        try {
            // Execute initialize() method to finish instantiation of the
            // repository.
            initMethod.invoke(instance);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to call initialize() in: " + clazz, e);
        }
    }
}
