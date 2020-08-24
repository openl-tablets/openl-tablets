package org.openl.rules.webstudio.web.admin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

public class RepositoryEditor {
    private final RepositoryFactoryProxy repositoryFactoryProxy;
    private final String repoListConfig;

    private final List<RepositoryConfiguration> repositoryConfigurations = new ArrayList<>();
    private final List<RepositoryConfiguration> deletedConfigurations = new ArrayList<>();

    private final PropertiesHolder properties;

    public RepositoryEditor(RepositoryFactoryProxy repositoryFactoryProxy,
            PropertiesHolder properties) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
        this.repoListConfig = repositoryFactoryProxy.getRepoListConfig();
        this.properties = createPropertiesWrapper(properties, repoListConfig);
    }

    static PropertiesHolder createPropertiesWrapper(PropertiesHolder properties, String repoListConfig) {
        String repoConfigName = getFirstConfigName(properties.getProperty(repoListConfig));

        return (PropertiesHolder) Proxy.newProxyInstance(properties.getClass().getClassLoader(),
            new Class[] { PropertiesHolder.class },
            createDefaultValueInvocationHandler(properties, repoConfigName));
    }

    public static PropertyResolver createPropertiesWrapper(PropertyResolver properties, String repoListConfig) {
        String repoConfigName = getFirstConfigName(properties.getProperty(repoListConfig));

        return (PropertyResolver) Proxy.newProxyInstance(properties.getClass().getClassLoader(),
            new Class[] { PropertyResolver.class },
            createDefaultValueInvocationHandler(properties, repoConfigName));
    }

    private static String getFirstConfigName(String configNames) {
        if (configNames == null || configNames.isEmpty()) {
            throw new IllegalArgumentException("Config names were not found");
        }
        return StringUtils.split(configNames, ',')[0];
    }

    private static InvocationHandler createDefaultValueInvocationHandler(Object properties, String repoConfigName) {
        return (proxy, method, args) -> {
            Object result = method.invoke(properties, args);

            if (result == null) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if ("getProperty"
                    .equals(method.getName()) && parameterTypes.length == 1 && parameterTypes[0] == String.class) {
                    // Not found default value. Let's get default value from first repository.
                    String key = (String) args[0];
                    String prefix = "repository.";
                    if (key.startsWith(prefix)) {
                        // Replace repository key to design repository
                        int from = prefix.length();
                        int to = key.indexOf('.', from);
                        String newKey = prefix + repoConfigName + key.substring(to);
                        result = method.invoke(properties, newKey);
                    }
                }
            }

            return result;
        };
    }

    public List<RepositoryConfiguration> getRepositoryConfigurations() {
        if (repositoryConfigurations.isEmpty()) {
            reload();
        }

        return repositoryConfigurations;
    }

    public void reload() {
        repositoryConfigurations.clear();

        String[] repositoryConfigNames = split(
            properties.getProperty(repoListConfig));
        for (String configName : repositoryConfigNames) {
            RepositoryConfiguration config = new RepositoryConfiguration(configName, properties);
            repositoryConfigurations.add(config);
        }
    }

    public void addRepository(RepositoryConfiguration configuration) {
        repositoryConfigurations.add(configuration);
    }

    public void deleteRepository(String configName) {
        deleteRepository(configName, null);
    }

    public void deleteRepository(String configName, Callback callback) {
        Iterator<RepositoryConfiguration> it = repositoryConfigurations.iterator();
        while (it.hasNext()) {
            RepositoryConfiguration prodConfig = it.next();
            if (prodConfig.getConfigName().equals(configName)) {
                deletedConfigurations.add(prodConfig);
                it.remove();

                if (callback != null) {
                    callback.onDelete(configName);
                }

                break;
            }
        }
    }

    public void validate() throws RepositoryValidationException {
        for (RepositoryConfiguration prodConfig : repositoryConfigurations) {
            RepositoryValidators.validate(prodConfig, repositoryConfigurations);
            RepositoryValidators.validateConnection(prodConfig, repositoryFactoryProxy);
        }
    }

    public void save() {
        save(null);
    }

    public void save(Callback callback) {
        for (RepositoryConfiguration prodConfig : deletedConfigurations) {
            if (callback != null) {
                callback.onDelete(prodConfig.getConfigName());
            }
            prodConfig.revert();
        }

        deletedConfigurations.clear();

        String[] configNames = new String[repositoryConfigurations.size()];
        for (int i = 0; i < repositoryConfigurations.size(); i++) {
            RepositoryConfiguration prodConfig = repositoryConfigurations.get(i);
            RepositoryConfiguration newProdConfig = saveProductionRepository(prodConfig);
            repositoryConfigurations.set(i, newProdConfig);
            configNames[i] = newProdConfig.getConfigName();
        }
        properties.setProperty(repoListConfig, String.join(",", configNames));
    }

    public void revertChanges() {
        for (RepositoryConfiguration prodConfig : deletedConfigurations) {
            prodConfig.revert();
        }
        deletedConfigurations.clear();

        for (RepositoryConfiguration productionRepositoryConfiguration : repositoryConfigurations) {
            productionRepositoryConfiguration.revert();
        }
        repositoryConfigurations.clear();

        properties.revertProperties(repoListConfig);
    }

    private RepositoryConfiguration saveProductionRepository(RepositoryConfiguration prodConfig) {
        prodConfig.commit();
        if (prodConfig.isNameChangedIgnoreCase()) {
            String newConfigName = prodConfig.getName();
            properties.setProperty("repository." + prodConfig.getConfigName() + ".name", newConfigName);
        }

        return prodConfig;
    }

    private String[] split(String s) {
        return StringUtils.split(s, ',');
    }

    public abstract static class Callback {
        public void onDelete(String configName) {
            // Do nothing
        }

        public void onRename(String oldConfigName, String newConfigName) {
            // Do nothing
        }
    }
}