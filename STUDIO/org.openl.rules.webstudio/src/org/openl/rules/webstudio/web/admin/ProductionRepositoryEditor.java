package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.util.StringUtils;

public class ProductionRepositoryEditor {
    private final ConfigurationManager configManager;
    private final ConfigurationManagerFactory productionConfigManagerFactory;
    private final ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    private List<RepositoryConfiguration> productionRepositoryConfigurations = new ArrayList<>();
    private List<RepositoryConfiguration> deletedConfigurations = new ArrayList<>();

    public ProductionRepositoryEditor(ConfigurationManager configManager,
            ConfigurationManagerFactory productionConfigManagerFactory,
            ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.configManager = configManager;
        this.productionConfigManagerFactory = productionConfigManagerFactory;
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        if (productionRepositoryConfigurations.isEmpty()) {
            reload();
        }

        return productionRepositoryConfigurations;
    }

    public void reload() {
        productionRepositoryConfigurations.clear();

        String[] repositoryConfigNames = split(configManager.getStringProperty(AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS));
        for (String configName : repositoryConfigNames) {
            ConfigurationManager productionConfig = getProductionConfigManager(configName);
            RepositoryConfiguration config = new RepositoryConfiguration(configName,
                    productionConfig,
                    RepositoryMode.PRODUCTION);
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
            prodConfig.delete();
        }

        deletedConfigurations.clear();

        String[] configNames = new String[productionRepositoryConfigurations.size()];
        for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
            RepositoryConfiguration prodConfig = productionRepositoryConfigurations.get(i);
            RepositoryConfiguration newProdConfig = saveProductionRepository(prodConfig, callback);
            productionRepositoryConfigurations.set(i, newProdConfig);
            configNames[i] = newProdConfig.getConfigName();
        }
        configManager.setProperty(PRODUCTION_REPOSITORY_CONFIGS, join(configNames));
    }

    public void revertChanges() {
        for (RepositoryConfiguration prodConfig : deletedConfigurations) {
            prodConfig.delete();
        }
        deletedConfigurations.clear();

        for (RepositoryConfiguration productionRepositoryConfiguration : productionRepositoryConfigurations) {
            productionRepositoryConfiguration.delete();
        }
        productionRepositoryConfigurations.clear();
    }

    private RepositoryConfiguration saveProductionRepository(RepositoryConfiguration prodConfig, Callback callback) {
        boolean changed = prodConfig.save();
        if (changed) {
            String oldConfigName = prodConfig.getConfigName();
            if (prodConfig.isNameChangedIgnoreCase()) {
                prodConfig = renameConfigName(prodConfig);
            }
            String newConfigName = prodConfig.getConfigName();

            if (callback != null) {
                callback.onRename(oldConfigName, newConfigName);
            }
        }
        return prodConfig;
    }

    private RepositoryConfiguration renameConfigName(RepositoryConfiguration prodConfig) {
        // Move config to a new file
        String newConfigName = getConfigName(prodConfig.getName());
        RepositoryConfiguration newConfig = new RepositoryConfiguration(newConfigName,
                getProductionConfigManager(newConfigName), RepositoryMode.PRODUCTION);
        newConfig.copyContent(prodConfig);
        newConfig.save();

        // Rename link to a file in system config
        String[] configNames = split(configManager
                .getStringProperty(AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS));
        for (int i = 0; i < configNames.length; i++) {
            if (configNames[i].equals(prodConfig.getConfigName())) {
                // Found necessary link - rename it
                configNames[i] = newConfigName;
                configManager.setProperty(AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS, join(configNames));
                break;
            }
        }

        // Delete old config file
        prodConfig.delete();

        return newConfig;
    }

    private ConfigurationManager getProductionConfigManager(String configName) {
        return productionConfigManagerFactory.getConfigurationManager(configName);
    }

    private String getConfigName(String repositoryName) {
        String configName = "rules-";
        if (repositoryName != null) {
            configName += repositoryName.toLowerCase();
        }
        configName += ".properties";

        return configName;
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