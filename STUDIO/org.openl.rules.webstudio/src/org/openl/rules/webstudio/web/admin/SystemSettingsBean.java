package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.*;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.ProductionRepositoriesTreeController;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

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

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    @ManagedProperty(value = "#{environment}")
    private Environment environment;

    private ConfigurationManager configManager;
    private RepositoryConfiguration designRepositoryConfiguration;
    private RepositoryConfiguration deployConfigRepositoryConfiguration;

    private ProductionRepositoryEditor productionRepositoryEditor;
    private SystemSettingsValidator validator;

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        try {
            configManager = WebStudioUtils.getWebStudio(true).getSystemConfigManager();

            designRepositoryConfiguration = new RepositoryConfiguration(RepositoryMode.DESIGN.name().toLowerCase(),
                environment);
            if (designRepositoryConfiguration.getErrorMessage() != null) {
                log.error(designRepositoryConfiguration.getErrorMessage());
                FacesUtils.addErrorMessage("Incorrect design repository configuration, please fix it.");
            }

            deployConfigRepositoryConfiguration = new RepositoryConfiguration(
                "deploy-config",
                environment);
            if (!isUseDesignRepo() && deployConfigRepositoryConfiguration.getErrorMessage() != null) {
                log.error(deployConfigRepositoryConfiguration.getErrorMessage());
                FacesUtils.addErrorMessage("Incorrect deploy config repository configuration, please fix it.");
            }

            productionRepositoryEditor = new ProductionRepositoryEditor(configManager,
                productionConfigManagerFactory,
                productionRepositoryFactoryProxy,
                environment);

            validator = new SystemSettingsValidator(this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    public boolean isHasMessages() {
        return FacesUtils.getFacesContext().getMaximumSeverity() != null;
    }

    public String getUserWorkspaceHome() {
        return environment.getProperty(USER_WORKSPACE_HOME);
    }

    public void setUserWorkspaceHome(String userWorkspaceHome) {
        configManager.setProperty(USER_WORKSPACE_HOME, userWorkspaceHome);
    }

    public String getDatePattern() {
        return environment.getProperty(DATE_PATTERN);
    }

    public void setDatePattern(String datePattern) {
        configManager.setProperty(DATE_PATTERN, datePattern);
    }

    public boolean isUpdateSystemProperties() {
        return Boolean.parseBoolean(environment.getProperty(UPDATE_SYSTEM_PROPERTIES));
    }

    public void setUpdateSystemProperties(boolean updateSystemProperties) {
        configManager.setProperty(UPDATE_SYSTEM_PROPERTIES, updateSystemProperties);
    }

    public String getProjectHistoryHome() {
        return environment.getProperty(PROJECT_HISTORY_HOME);
    }

    public void setProjectHistoryHome(String projectHistoryHome) {
        configManager.setProperty(PROJECT_HISTORY_HOME, projectHistoryHome);
    }

    public String getProjectHistoryCount() {
        if (isUnlimitHistory()) {
            return "0";
        } else {
            return environment.getProperty(PROJECT_HISTORY_COUNT);
        }
    }

    public void setProjectHistoryCount(String count) {
        configManager.setProperty(PROJECT_HISTORY_COUNT, Integer.parseInt(count));
    }

    public boolean isUnlimitHistory() {
        return Boolean.parseBoolean(environment.getProperty(PROJECT_HISTORY_UNLIMITED));
    }

    public void setUnlimitHistory(boolean unlimited) {
        configManager.setProperty(PROJECT_HISTORY_UNLIMITED, unlimited);
    }

    public RepositoryConfiguration getDesignRepositoryConfiguration() {
        return designRepositoryConfiguration;
    }

    public RepositoryConfiguration getDeployConfigRepositoryConfiguration() {
        return deployConfigRepositoryConfiguration;
    }

    public boolean isUseDesignRepo() {
        return !Boolean.parseBoolean(environment.getProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO));
    }

    public void setUseDesignRepo(boolean useDesignRepo) {
        configManager.setProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO, !useDesignRepo);
    }

    public FolderStructureSettings getDesignFolderStructure() {
        return new FolderStructureSettings(configManager, RepositoryMode.DESIGN);
    }

    public FolderStructureSettings getDeployConfigFolderStructure() {
        return new FolderStructureSettings(configManager, RepositoryMode.DEPLOY_CONFIG);
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryEditor.getProductionRepositoryConfigurations();
    }

    public void setDispatchingValidationEnabled(boolean dispatchingValidationEnabled) {
        configManager.setProperty(OpenLSystemProperties.DISPATCHING_VALIDATION, dispatchingValidationEnabled);
    }

    public boolean isDispatchingValidationEnabled() {
        return Boolean.parseBoolean(environment.getProperty(OpenLSystemProperties.DISPATCHING_VALIDATION));
    }

    public boolean isRunTestsInParallel() {
        return Boolean.parseBoolean(environment.getProperty(OpenLSystemProperties.RUN_TESTS_IN_PARALLEL));
    }

    public void setRunTestsInParallel(boolean runTestsInParallel) {
        configManager.setProperty(OpenLSystemProperties.RUN_TESTS_IN_PARALLEL, runTestsInParallel);
    }

    public String getTestRunThreadCount() {
        return environment.getProperty(OpenLSystemProperties.TEST_RUN_THREAD_COUNT_PROPERTY);
    }

    public void setTestRunThreadCount(String testRunThreadCount) {
        configManager.setProperty(OpenLSystemProperties.TEST_RUN_THREAD_COUNT_PROPERTY,
            Integer.parseInt(StringUtils.trim(testRunThreadCount)));
    }

    public boolean isAutoCompile() {
        return Boolean.parseBoolean(environment.getProperty(OpenLSystemProperties.AUTO_COMPILE));
    }

    public void setAutoCompile(boolean autoCompile) {
        configManager.setProperty(OpenLSystemProperties.AUTO_COMPILE, autoCompile);
    }

    public void applyChanges() {
        try {
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();

            RepositoryValidators.validate(designRepositoryConfiguration);
            RepositoryValidators.validateConnectionForDesignRepository(designRepositoryConfiguration,
                designTimeRepository,
                RepositoryMode.DESIGN);

            if (!isUseDesignRepo()) {
                RepositoryValidators.validate(deployConfigRepositoryConfiguration);
                RepositoryValidators.validateConnectionForDesignRepository(deployConfigRepositoryConfiguration,
                    designTimeRepository,
                    RepositoryMode.DEPLOY_CONFIG);
            }

            productionRepositoryEditor.validate();
            productionRepositoryEditor.save(new ProductionRepositoryEditor.Callback() {
                @Override
                public void onDelete(String configName) {
                    deploymentManager.removeRepository(configName);
                }

                @Override
                public void onRename(String oldConfigName, String newConfigName) {
                    deploymentManager.removeRepository(oldConfigName);
                    deploymentManager.addRepository(newConfigName);
                }
            });

            saveSystemConfig();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    private void saveSystemConfig() {
        // TODO: This line also do configManager.save() implicitly
        boolean saved = designRepositoryConfiguration.save();
        if (!isUseDesignRepo()) {
            saved &= deployConfigRepositoryConfiguration.save();
        }

        if (saved) {
            refreshConfig();
        }
    }

    public void restoreDefaults() {
        designRepositoryConfiguration.revert();

        configManager.revertProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO);
        deployConfigRepositoryConfiguration.revert();

        productionRepositoryEditor.revertChanges();

        // We cannot invoke configManager.restoreDefaults(): in this case some
        // settings (such as user.mode etc) not edited in this page
        // will be reverted too. We should revert only settings edited in Administration page
        for (String setting : AdministrationSettings.getAllSettings()) {
            configManager.revertProperty(setting);
        }
        saveSystemConfig();

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

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void deleteProductionRepository(String configName) {
        try {
            productionRepositoryEditor.deleteProductionRepository(configName,
                new ProductionRepositoryEditor.Callback() {
                    @Override
                    public void onDelete(String configName) {
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

    private void refreshConfig() {
        WebStudioUtils.getWebStudio().setNeedRestart(true);
        ReloadableDelegatingFilter.reloadApplicationContext(FacesUtils.getServletContext());
    }

}
