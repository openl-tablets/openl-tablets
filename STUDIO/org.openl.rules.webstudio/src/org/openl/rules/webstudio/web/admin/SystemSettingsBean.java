package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.*;

import java.util.List;

import javax.annotation.PostConstruct;
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
import org.openl.rules.workspace.dtr.DesignTimeRepository;
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
    private final Logger log = LoggerFactory.getLogger(SystemSettingsBean.class);

    @ManagedProperty(value = "#{productionRepositoriesTreeController}")
    private ProductionRepositoriesTreeController productionRepositoriesTreeController;

    @ManagedProperty(value = "#{productionRepositoryFactoryProxy}")
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    @ManagedProperty(value = "#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;

    @ManagedProperty(value = "#{deploymentManager}")
    private DeploymentManager deploymentManager;

    @ManagedProperty(value = "#{designTimeRepository}")
    private DesignTimeRepository designTimeRepository;

    private ConfigurationManager configManager;
    private RepositoryConfiguration designRepositoryConfiguration;

    private ProductionRepositoryEditor productionRepositoryEditor;
    private SystemSettingsValidator validator;

    @PostConstruct
    public void afterPropertiesSet() {
        configManager = WebStudioUtils.getWebStudio(true).getSystemConfigManager();

        designRepositoryConfiguration = new RepositoryConfiguration("", configManager, RepositoryType.DESIGN);

        productionRepositoryEditor = new ProductionRepositoryEditor(configManager,
                productionConfigManagerFactory,
                productionRepositoryFactoryProxy);

        validator = new SystemSettingsValidator(this);
    }

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
        return productionRepositoryEditor.getProductionRepositoryConfigurations();
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

    public void applyChanges() {
        try {
            RepositoryValidators.validate(designRepositoryConfiguration);
            RepositoryValidators.validateConnectionForDesignRepository(designRepositoryConfiguration, designTimeRepository);

            productionRepositoryEditor.validate();
            productionRepositoryEditor.save(new ProductionRepositoryEditor.Callback() {
                @Override public void onDelete(String configName) throws RRepositoryException {
                    deploymentManager.removeRepository(configName);
                }

                @Override public void onRename(String oldConfigName, String newConfigName) {
                    try {
                        deploymentManager.removeRepository(oldConfigName);
                    } catch (RRepositoryException e) {
                        log.error(e.getMessage(), e);
                    }

                    deploymentManager.addRepository(newConfigName);
                }
            });

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
        productionRepositoryEditor.revertChanges();

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

        productionRepositoryEditor.reload();
    }

    public void setProductionConfigManagerFactory(ConfigurationManagerFactory productionConfigManagerFactory) {
        this.productionConfigManagerFactory = productionConfigManagerFactory;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void setDesignTimeRepository(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    public void deleteProductionRepository(String configName) {
        try {
            productionRepositoryEditor.deleteProductionRepository(configName, new ProductionRepositoryEditor.Callback() {
                @Override public void onDelete(String configName) throws RRepositoryException {
                    /* Delete Production repo from tree */
                    productionRepositoriesTreeController.deleteProdRepo(configName);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage(e.getMessage());
        }
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

}
