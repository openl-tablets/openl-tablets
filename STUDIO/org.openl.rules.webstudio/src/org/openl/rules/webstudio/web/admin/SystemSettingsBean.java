package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.*;

import java.io.IOException;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.openl.config.ConfigNames;
import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.security.AccessManager;
import org.openl.rules.security.Privileges;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.ProductionRepositoriesTreeController;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Controller;

/**
 * TODO Remove property getters/setters when migrating to EL 2.2
 *
 * @author Andrei Astrouski
 */
@Controller
@ViewScope
public class SystemSettingsBean {
    private final Logger log = LoggerFactory.getLogger(SystemSettingsBean.class);

    private final ProductionRepositoriesTreeController productionRepositoriesTreeController;

    private final DeploymentManager deploymentManager;

    private final DesignTimeRepository designTimeRepository;

    private final RepositoryTreeState repositoryTreeState;

    private PropertiesHolder properties;

    private RepositoryConfiguration designRepositoryConfiguration;
    private RepositoryConfiguration deployConfigRepositoryConfiguration;

    private ProductionRepositoryEditor productionRepositoryEditor;
    private SystemSettingsValidator validator;

    public SystemSettingsBean(ProductionRepositoriesTreeController productionRepositoriesTreeController,
        ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy,
        DeploymentManager deploymentManager,
        DesignTimeRepository designTimeRepository,
        RepositoryTreeState repositoryTreeState,
        PropertyResolver propertyResolver) {
        this.productionRepositoriesTreeController = productionRepositoriesTreeController;
        this.deploymentManager = deploymentManager;
        this.designTimeRepository = designTimeRepository;
        this.repositoryTreeState = repositoryTreeState;

        properties = new InMemoryProperties(propertyResolver);

        try {
            designRepositoryConfiguration = new RepositoryConfiguration(ConfigNames.DESIGN_CONFIG, properties);
            if (designRepositoryConfiguration.getErrorMessage() != null) {
                log.error(designRepositoryConfiguration.getErrorMessage());
                WebStudioUtils.addErrorMessage("Incorrect design repository configuration, please fix it.");
            }

            deployConfigRepositoryConfiguration = new RepositoryConfiguration(ConfigNames.DEPLOY_CONFIG, properties);
            if (!isUseDesignRepo() && deployConfigRepositoryConfiguration.getErrorMessage() != null) {
                log.error(deployConfigRepositoryConfiguration.getErrorMessage());
                WebStudioUtils.addErrorMessage("Incorrect deploy config repository configuration, please fix it.");
            }

            productionRepositoryEditor = new ProductionRepositoryEditor(productionRepositoryFactoryProxy, properties);

            validator = new SystemSettingsValidator(this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage(e.getMessage());
        }
    }

    public boolean isHasMessages() {
        return FacesContext.getCurrentInstance().getMaximumSeverity() != null;
    }

    public String getUserWorkspaceHome() {
        return properties.getProperty(USER_WORKSPACE_HOME);
    }

    public void setUserWorkspaceHome(String userWorkspaceHome) {
        properties.setProperty(USER_WORKSPACE_HOME, userWorkspaceHome);
    }

    public String getDatePattern() {
        return properties.getProperty(DATE_PATTERN);
    }

    public void setDatePattern(String datePattern) {
        properties.setProperty(DATE_PATTERN, datePattern);
    }

    public String getTimePattern() {
        return properties.getProperty(TIME_PATTERN);
    }

    public void setTimePattern(String timePattern) {
        properties.setProperty(TIME_PATTERN, timePattern);
    }

    public boolean isUpdateSystemProperties() {
        return Boolean.parseBoolean(properties.getProperty(UPDATE_SYSTEM_PROPERTIES));
    }

    public void setUpdateSystemProperties(boolean updateSystemProperties) {
        properties.setProperty(UPDATE_SYSTEM_PROPERTIES, updateSystemProperties);
    }

    public String getProjectHistoryHome() {
        return properties.getProperty(PROJECT_HISTORY_HOME);
    }

    public void setProjectHistoryHome(String projectHistoryHome) {
        properties.setProperty(PROJECT_HISTORY_HOME, projectHistoryHome);
    }

    public String getProjectHistoryCount() {
        if (isUnlimitHistory()) {
            return "0";
        } else {
            return properties.getProperty(PROJECT_HISTORY_COUNT);
        }
    }

    public void setProjectHistoryCount(String count) {
        properties.setProperty(PROJECT_HISTORY_COUNT, Integer.parseInt(count));
    }

    public boolean isUnlimitHistory() {
        return Boolean.parseBoolean(properties.getProperty(PROJECT_HISTORY_UNLIMITED));
    }

    public void setUnlimitHistory(boolean unlimited) {
        properties.setProperty(PROJECT_HISTORY_UNLIMITED, unlimited);
    }

    public RepositoryConfiguration getDesignRepositoryConfiguration() {
        return designRepositoryConfiguration;
    }

    public RepositoryConfiguration getDeployConfigRepositoryConfiguration() {
        return deployConfigRepositoryConfiguration;
    }

    public boolean isUseDesignRepo() {
        return !Boolean.parseBoolean(properties.getProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO));
    }

    public void setUseDesignRepo(boolean useDesignRepo) {
        properties.setProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO, !useDesignRepo);
    }

    public FolderStructureSettings getDesignFolderStructure() {
        return new FolderStructureSettings(designRepositoryConfiguration);
    }

    public FolderStructureSettings getDeployConfigFolderStructure() {
        return new FolderStructureSettings(deployConfigRepositoryConfiguration);
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryEditor.getProductionRepositoryConfigurations();
    }

    public PropertiesHolder getProperties() {
        return properties;
    }

    public void setDispatchingValidationEnabled(boolean dispatchingValidationEnabled) {
        properties.setProperty(OpenLSystemProperties.DISPATCHING_VALIDATION, dispatchingValidationEnabled);
    }

    public boolean isDispatchingValidationEnabled() {
        return Boolean.parseBoolean(properties.getProperty(OpenLSystemProperties.DISPATCHING_VALIDATION));
    }

    public boolean isRunTestsInParallel() {
        return Boolean.parseBoolean(properties.getProperty(RUN_TESTS_IN_PARALLEL));
    }

    public void setRunTestsInParallel(boolean runTestsInParallel) {
        properties.setProperty(RUN_TESTS_IN_PARALLEL, runTestsInParallel);
    }

    public String getTestRunThreadCount() {
        return properties.getProperty(TEST_RUN_THREAD_COUNT_PROPERTY);
    }

    public void setTestRunThreadCount(String testRunThreadCount) {
        properties.setProperty(TEST_RUN_THREAD_COUNT_PROPERTY, Integer.parseInt(StringUtils.trim(testRunThreadCount)));
    }

    public boolean isAutoCompile() {
        return Boolean.parseBoolean(properties.getProperty(AUTO_COMPILE));
    }

    public boolean isAdmin() {
        return AccessManager.isGranted(Privileges.ADMIN);
    }

    public void setAutoCompile(boolean autoCompile) {
        properties.setProperty(AUTO_COMPILE, autoCompile);
    }

    public void applyChanges() {
        try {
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();

            RepositoryValidators.validate(designRepositoryConfiguration);
            RepositoryValidators.validateConnectionForDesignRepository(designRepositoryConfiguration,
                designTimeRepository);

            if (!isUseDesignRepo()) {
                RepositoryValidators.validate(deployConfigRepositoryConfiguration);
                RepositoryValidators.validateConnectionForDesignRepository(deployConfigRepositoryConfiguration,
                    designTimeRepository);
            }

            productionRepositoryEditor.validate();
            productionRepositoryEditor.save(new ProductionRepositoryEditor.Callback() {
                @Override
                public void onDelete(String configName) {
                    deploymentManager.removeRepository(configName);
                }
            });

            saveSystemConfig();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage(e.getMessage());
        }
    }

    private void saveSystemConfig() throws IOException {
        designRepositoryConfiguration.commit();
        if (!isUseDesignRepo()) {
            deployConfigRepositoryConfiguration.commit();
        }

        DynamicPropertySource.get().save(properties.getConfig());

        refreshConfig();
    }

    public void restoreDefaults() {
        try {
            designRepositoryConfiguration.revert();
            properties.revertProperties(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO);
            deployConfigRepositoryConfiguration.revert();

            productionRepositoryEditor.revertChanges();

            // We cannot invoke configManager.restoreDefaults(): in this case some
            // settings (such as user.mode etc) not edited in this page
            // will be reverted too. We should revert only settings edited in Administration page
            properties
                .revertProperties(AdministrationSettings.getAllSettings().toArray(StringUtils.EMPTY_STRING_ARRAY));
            saveSystemConfig();

            productionRepositoryEditor.reload();
            designRepositoryConfiguration.reload();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage(e.getMessage());
        }
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
            WebStudioUtils.addErrorMessage(e.getMessage());
        }
    }

    public SystemSettingsValidator getValidator() {
        return validator;
    }

    private void refreshConfig() {
        WebStudioUtils.getWebStudio(true).setNeedRestart(true);
        ReloadableDelegatingFilter
            .reloadApplicationContext((ServletContext) WebStudioUtils.getExternalContext().getContext());
    }

}
