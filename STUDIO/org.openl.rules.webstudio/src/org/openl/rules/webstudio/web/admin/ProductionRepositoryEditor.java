package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.util.StringUtils;

public class ProductionRepositoryEditor {
    private final ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    private List<RepositoryConfiguration> productionRepositoryConfigurations = new ArrayList<>();
    private List<RepositoryConfiguration> deletedConfigurations = new ArrayList<>();

    private PropertiesHolder properties;

    public ProductionRepositoryEditor(ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy,
        PropertiesHolder properties) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
        this.properties = properties;
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        if (productionRepositoryConfigurations.isEmpty()) {
            reload();
        }

        return productionRepositoryConfigurations;
    }

    public void reload() {
        productionRepositoryConfigurations.clear();

        String[] repositoryConfigNames = split(
            properties.getProperty(AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS));
        for (String configName : repositoryConfigNames) {
            RepositoryConfiguration config = new RepositoryConfiguration(configName, properties);
            productionRepositoryConfigurations.add(config);
        }
    }

    public void deleteProductionRepository(String configName) {
        deleteProductionRepository(configName, null);
    }

    public void deleteProductionRepository(String configName, Callback callback) {
        Iterator<RepositoryConfiguration> it = productionRepositoryConfigurations.iterator();
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
        for (RepositoryConfiguration prodConfig : productionRepositoryConfigurations) {
            RepositoryValidators.validate(prodConfig, productionRepositoryConfigurations);
            RepositoryValidators.validateConnection(prodConfig, productionRepositoryFactoryProxy);
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

        String[] configNames = new String[productionRepositoryConfigurations.size()];
        for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
            RepositoryConfiguration prodConfig = productionRepositoryConfigurations.get(i);
            RepositoryConfiguration newProdConfig = saveProductionRepository(prodConfig, callback);
            productionRepositoryConfigurations.set(i, newProdConfig);
            configNames[i] = newProdConfig.getConfigName();
        }
        properties.setProperty(PRODUCTION_REPOSITORY_CONFIGS, join(configNames));
    }

    public void revertChanges() {
        for (RepositoryConfiguration prodConfig : deletedConfigurations) {
            prodConfig.revert();
        }
        deletedConfigurations.clear();

        for (RepositoryConfiguration productionRepositoryConfiguration : productionRepositoryConfigurations) {
            productionRepositoryConfiguration.revert();
        }
        productionRepositoryConfigurations.clear();

        properties.revertProperty(AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS);
    }

    private RepositoryConfiguration saveProductionRepository(RepositoryConfiguration prodConfig, Callback callback) {
        prodConfig.commit();
        String oldConfigName = prodConfig.getConfigName();
        if (prodConfig.isNameChangedIgnoreCase()) {
            prodConfig = renameConfigName(prodConfig);
        }
        String newConfigName = prodConfig.getConfigName();

        if (callback != null) {
            callback.onRename(oldConfigName, newConfigName);
        }
        return prodConfig;
    }

    private RepositoryConfiguration renameConfigName(RepositoryConfiguration prodConfig) {
        // Move config to a new file
        String newConfigName = prodConfig.getName();
        RepositoryConfiguration newConfig = new RepositoryConfiguration(newConfigName, properties);
        newConfig.copyContent(prodConfig);
        newConfig.commit();

        // Rename link to a file in system config
        String[] configNames = split(
            properties.getProperty(AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS));
        for (int i = 0; i < configNames.length; i++) {
            if (configNames[i].equals(prodConfig.getConfigName())) {
                // Found necessary link - rename it
                configNames[i] = newConfigName;
                properties.setProperty(AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS, join(configNames));
                break;
            }
        }

        // Delete old config file
        prodConfig.revert();

        return newConfig;
    }

    private String[] split(String s) {
        return StringUtils.split(s, ',');
    }

    private String join(String[] arr) {
        return StringUtils.join(arr, ",");
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