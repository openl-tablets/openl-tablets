package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter.ConfigurationReloader;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.ProductionRepositoriesTreeController;
import org.openl.rules.webstudio.web.servlet.SessionListener;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * TODO Remove property getters/setters when migrating to EL 2.2
 *
 * @author Andrei Astrouski
 */
@ManagedBean
@ViewScoped
public class SystemSettingsBean {
    @ManagedProperty(value = "#{productionRepositoriesTreeController}")
    private ProductionRepositoriesTreeController productionRepositoriesTreeController;

    @ManagedProperty(value = "#{productionRepositoryFactoryProxy}")
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    private final Logger log = LoggerFactory.getLogger(SystemSettingsBean.class);

    private ConfigurationManager configManager = WebStudioUtils.getWebStudio(true).getSystemConfigManager();
    private RepositoryConfiguration designRepositoryConfiguration = new RepositoryConfiguration("",
            configManager,
            RepositoryType.DESIGN);

    private List<RepositoryConfiguration> productionRepositoryConfigurations = new ArrayList<RepositoryConfiguration>();
    private List<RepositoryConfiguration> deletedConfigurations = new ArrayList<RepositoryConfiguration>();

    @ManagedProperty(value = "#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;

    @ManagedProperty(value = "#{deploymentManager}")
    private DeploymentManager deploymentManager;

    private final SystemSettingsValidator validator = new SystemSettingsValidator(this);

    public String getUserWorkspaceHome() {
        return configManager.getPath(USER_WORKSPACE_HOME);
    }

    public void setUserWorkspaceHome(String userWorkspaceHome) {
        configManager.setPath(USER_WORKSPACE_HOME, userWorkspaceHome);
    }

    public String getDatePattern() {
        return configManager.getStringProperty(DATE_PATTERN);
    }

    public void setDatePattern(String datePattern) {
        configManager.setProperty(DATE_PATTERN, datePattern);
    }

    public boolean isUpdateSystemProperties() {
        return configManager.getBooleanProperty(UPDATE_SYSTEM_PROPERTIES);
    }

    public void setUpdateSystemProperties(boolean updateSystemProperties) {
        configManager.setProperty(UPDATE_SYSTEM_PROPERTIES, updateSystemProperties);
    }

    public String getProjectHistoryHome() {
        return configManager.getPath(PROJECT_HISTORY_HOME);
    }

    public void setProjectHistoryHome(String projectHistoryHome) {
        configManager.setPath(PROJECT_HISTORY_HOME, projectHistoryHome);
    }

    public String getProjectHistoryCount() {
        if (isUnlimitHistory()) {
            return "0";
        } else {
            return Integer.toString(configManager.getIntegerProperty(PROJECT_HISTORY_COUNT));
        }
    }

    public void setProjectHistoryCount(String count) {
        configManager.setProperty(PROJECT_HISTORY_COUNT, Integer.parseInt(count));
    }

    public boolean isUnlimitHistory() {
        return configManager.getBooleanProperty(PROJECT_HISTORY_UNLIMITED);
    }

    public void setUnlimitHistory(boolean unlimited) {
        configManager.setProperty(PROJECT_HISTORY_UNLIMITED, unlimited);
    }

    public RepositoryConfiguration getDesignRepositoryConfiguration() {
        return designRepositoryConfiguration;
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        if (productionRepositoryConfigurations.isEmpty()) {
            initProductionRepositoryConfigurations();
        }

        return productionRepositoryConfigurations;
    }

    private void initProductionRepositoryConfigurations() {
        productionRepositoryConfigurations.clear();

        String[] repositoryConfigNames = split(configManager.getStringProperty(PRODUCTION_REPOSITORY_CONFIGS));
        for (String configName : repositoryConfigNames) {
            ConfigurationManager productionConfig = getProductionConfigManager(configName);
            RepositoryConfiguration config = new RepositoryConfiguration(configName, productionConfig,
                    RepositoryType.PRODUCTION);
            productionRepositoryConfigurations.add(config);
        }
    }

    public boolean isCustomSpreadsheetType() {
        return OpenLSystemProperties.isCustomSpreadsheetType(configManager.getProperties());
    }

    public void setCustomSpreadsheetType(boolean customSpreadsheetType) {
        configManager.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, customSpreadsheetType);
    }

    public boolean isRunTestsInParallel() {
        return OpenLSystemProperties.isRunTestsInParallel(configManager.getProperties());
    }

    public void setRunTestsInParallel(boolean runTestsInParallel) {
        configManager.setProperty(OpenLSystemProperties.RUN_TESTS_IN_PARALLEL, runTestsInParallel);
    }

    public String getTestRunThreadCount() {
        return Integer.toString(OpenLSystemProperties.getTestRunThreadCount(configManager.getProperties()));
    }

    public void setTestRunThreadCount(String testRunThreadCount) {
        configManager.setProperty(OpenLSystemProperties.TEST_RUN_THREAD_COUNT_PROPERTY, Integer.parseInt(StringUtils.trim(testRunThreadCount)));
    }

    public String getRulesDispatchingMode() {
        return OpenLSystemProperties.getDispatchingMode(configManager.getProperties());
    }

    public void setRulesDispatchingMode(String dispatchingMode) {
        configManager.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, dispatchingMode);
    }

    public String getMaxCachedProjectsCount() {
        return Integer.toString(configManager.getIntegerProperty(MAX_CACHED_PROJECTS_COUNT));
    }

    public void setMaxCachedProjectsCount(String count) {
        int value = StringUtils.isBlank(count) ? 0 : Integer.parseInt(StringUtils.trim(count));
        configManager.setProperty(MAX_CACHED_PROJECTS_COUNT, value);
    }

    public String getCachedProjectIdleTime() {
        return Integer.toString(configManager.getIntegerProperty(CACHED_PROJECT_IDLE_TIME));
    }

    public void setCachedProjectIdleTime(String count) {
        int value = StringUtils.isBlank(count) ? 0 : Integer.parseInt(StringUtils.trim(count));
        configManager.setProperty(CACHED_PROJECT_IDLE_TIME, value);
    }

    public void applyChanges() {
        try {
            for (RepositoryConfiguration prodConfig : productionRepositoryConfigurations) {
                RepositoryValidators.validate(prodConfig, productionRepositoryConfigurations);
                RepositoryValidators.validateConnection(prodConfig, productionRepositoryFactoryProxy);
            }

            for (RepositoryConfiguration prodConfig : deletedConfigurations) {
                deploymentManager.removeRepository(prodConfig.getConfigName());
                prodConfig.delete();
            }

            deletedConfigurations.clear();

            String configNames[] = new String[productionRepositoryConfigurations.size()];
            for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
                RepositoryConfiguration prodConfig = productionRepositoryConfigurations.get(i);
                RepositoryConfiguration newProdConfig = saveProductionRepository(prodConfig);
                productionRepositoryConfigurations.set(i, newProdConfig);
                configNames[i] = newProdConfig.getConfigName();
            }
            configManager.setProperty(PRODUCTION_REPOSITORY_CONFIGS, join(configNames));

            saveSystemConfig();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    private void saveSystemConfig() throws ServletException {
        boolean saved = configManager.save();
        if (saved) {
            refreshConfig();
        }
    }

    public void restoreDefaults() throws ServletException {
        for (RepositoryConfiguration prodConfig : deletedConfigurations) {
            prodConfig.delete();
        }
        deletedConfigurations.clear();

        for (RepositoryConfiguration productionRepositoryConfiguration : productionRepositoryConfigurations) {
            productionRepositoryConfiguration.delete();
        }
        productionRepositoryConfigurations.clear();

        // We cannot invoke configManager.restoreDefaults(): in this case some 
        // settings (such as user.mode, deployment.format.old etc) not edited in this page
        // will be reverted too. We should revert only settings edited in Administration page
        for (String setting : AdministrationSettings.getAllSettings()) {
            configManager.removeProperty(setting);
        }
        boolean restored = configManager.save();
        if (restored) {
            refreshConfig();
        }

        initProductionRepositoryConfigurations();
    }

    public void setProductionConfigManagerFactory(ConfigurationManagerFactory productionConfigManagerFactory) {
        this.productionConfigManagerFactory = productionConfigManagerFactory;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void deleteProductionRepository(String configName) {
        try {
            Iterator<RepositoryConfiguration> it = productionRepositoryConfigurations.iterator();
            while (it.hasNext()) {
                RepositoryConfiguration prodConfig = it.next();
                if (prodConfig.getConfigName().equals(configName)) {
                    deletedConfigurations.add(prodConfig);
                    it.remove();
                    /* Delete Production repo from tree */
                    productionRepositoriesTreeController.deleteProdRepo(prodConfig.getName());
                    break;
                }
            }

            // FacesUtils.addInfoMessage("Repository '" + repositoryName +
            // "' is deleted successfully");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    private RepositoryConfiguration saveProductionRepository(RepositoryConfiguration prodConfig)
            throws ServletException {
        boolean changed = prodConfig.save();
        if (changed) {
            String oldConfigName = prodConfig.getConfigName();
            if (prodConfig.isNameChangedIgnoreCase()) {
                prodConfig = renameConfigName(prodConfig);
            }
            String newConfigName = prodConfig.getConfigName();

            try {
                deploymentManager.removeRepository(oldConfigName);
            } catch (RRepositoryException e) {
                log.error(e.getMessage(), e);
            }

            deploymentManager.addRepository(newConfigName);
        }
        return prodConfig;
    }

    private RepositoryConfiguration renameConfigName(RepositoryConfiguration prodConfig) throws ServletException {
        // Move config to a new file
        String newConfigName = getConfigName(prodConfig.getName());
        RepositoryConfiguration newConfig = new RepositoryConfiguration(newConfigName,
                getProductionConfigManager(newConfigName), RepositoryType.PRODUCTION);
        newConfig.copyContent(prodConfig);
        newConfig.save();

        // Rename link to a file in system config
        String[] configNames = split(configManager.getStringProperty(PRODUCTION_REPOSITORY_CONFIGS));
        for (int i = 0; i < configNames.length; i++) {
            if (configNames[i].equals(prodConfig.getConfigName())) {
                // Found necessary link - rename it
                configNames[i] = newConfigName;
                configManager.setProperty(PRODUCTION_REPOSITORY_CONFIGS, join(configNames));
                saveSystemConfig();
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

    public SystemSettingsValidator getValidator() {
        return validator;
    }

    public void setProductionRepositoriesTreeController(
            ProductionRepositoriesTreeController productionRepositoriesTreeController) {
        this.productionRepositoriesTreeController = productionRepositoriesTreeController;
    }

    public void setProductionRepositoryFactoryProxy(ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    private void refreshConfig() throws ServletException {
        WebStudioUtils.getWebStudio().setNeedRestart(true);
        final ServletContext servletContext = FacesUtils.getServletContext();

        ReloadableDelegatingFilter.reload(new ConfigurationReloader() {

            @Override
            public void reload() {
                XmlWebApplicationContext context = (XmlWebApplicationContext) WebApplicationContextUtils
                        .getWebApplicationContext(servletContext);
                context.refresh();

                SessionListener.getSessionCache(servletContext).invalidateAll();
            }
        });
    }

    private String[] split(String s) {
        return StringUtils.split(s, ",");
    }

    private String join(String arr[]) {
        return StringUtils.join(arr, ",");
    }
}
