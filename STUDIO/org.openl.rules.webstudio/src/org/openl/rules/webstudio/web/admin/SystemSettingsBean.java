package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.AUTO_COMPILE;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.DATE_PATTERN;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.PROJECT_HISTORY_COUNT;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.TEST_RUN_THREAD_COUNT_PROPERTY;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.TIME_PATTERN;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.UPDATE_SYSTEM_PROPERTIES;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.USER_WORKSPACE_HOME;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import javax.faces.context.FacesContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.security.AccessManager;
import org.openl.rules.security.Privileges;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.ProductionRepositoriesTreeController;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;

/**
 * TODO Remove property getters/setters when migrating to EL 2.2
 *
 * @author Andrei Astrouski
 *
 * @deprecated Should be removed after migration to React Admin UI
 */
@Service
@SessionScope
@Deprecated(forRemoval = true)
public class SystemSettingsBean {
    private final Logger log = LoggerFactory.getLogger(SystemSettingsBean.class);

    private final ProductionRepositoriesTreeController productionRepositoriesTreeController;
    private final DeploymentManager deploymentManager;
    private final DesignTimeRepository designTimeRepository;
    private final RepositoryTreeState repositoryTreeState;
    private final RepositoryFactoryProxy designRepositoryFactoryProxy;
    private final RepositoryFactoryProxy productionRepositoryFactoryProxy;
    private final PropertyResolver propertyResolver;
    private final SystemSettingsValidator validator;

    private InMemoryProperties properties;
    private RepositoryConfiguration deployConfigRepositoryConfiguration;
    private RepositoryEditor designRepositoryEditor;
    private RepositoryEditor productionRepositoryEditor;
    private final RepositoryAclService designRepositoryAclService;

    public SystemSettingsBean(ProductionRepositoriesTreeController productionRepositoriesTreeController,
                              RepositoryFactoryProxy designRepositoryFactoryProxy,
                              RepositoryFactoryProxy productionRepositoryFactoryProxy,
                              DeploymentManager deploymentManager,
                              DesignTimeRepository designTimeRepository,
                              RepositoryTreeState repositoryTreeState,
                              PropertyResolver propertyResolver,
                              @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService) {
        this.productionRepositoriesTreeController = productionRepositoriesTreeController;
        this.deploymentManager = deploymentManager;
        this.designTimeRepository = designTimeRepository;
        this.repositoryTreeState = repositoryTreeState;
        this.designRepositoryFactoryProxy = designRepositoryFactoryProxy;
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
        this.propertyResolver = propertyResolver;
        this.designRepositoryAclService = designRepositoryAclService;
        this.validator = new SystemSettingsValidator();
        initialize();
    }

    public void initialize() {
        // We don't want to use application-scoped InMemoryProperties singleton, because we need to destroy all
        // unsaved changes when a user leaves the page. So we create a new InMemoryProperties in view-scoped
        // SystemSettingsBean constructor.
        this.properties = new InMemoryProperties(propertyResolver);

        try {
            deployConfigRepositoryConfiguration = new RepositoryConfiguration(RepositoryMode.DEPLOY_CONFIG.getId(),
                    properties);
            if (!isUseDesignRepo() && deployConfigRepositoryConfiguration.getErrorMessage() != null) {
                log.error(deployConfigRepositoryConfiguration.getErrorMessage());
                WebStudioUtils.addErrorMessage("Incorrect deploy config repository configuration, please fix it.");
            }

            designRepositoryEditor = new RepositoryEditor(designRepositoryFactoryProxy, properties);
            productionRepositoryEditor = new RepositoryEditor(productionRepositoryFactoryProxy, properties);

            for (RepositoryConfiguration configuration : designRepositoryEditor.getRepositoryConfigurations()) {
                if (configuration.getErrorMessage() != null) {
                    log.error(configuration.getErrorMessage());
                    WebStudioUtils.addErrorMessage(
                            "Incorrect design repository configuration '" + configuration.getName() + "'. Fix the errors and try again.");
                }
            }
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

    public String getProjectHistoryCount() {
        return properties.getProperty(PROJECT_HISTORY_COUNT);
    }

    public void setProjectHistoryCount(String count) {
        properties.setProperty(PROJECT_HISTORY_COUNT, count);
    }

    public List<RepositoryConfiguration> getDesignRepositoryConfigurations() {
        return designRepositoryEditor.getRepositoryConfigurations();
    }

    public RepositoryConfiguration getDeployConfigRepositoryConfiguration() {
        return deployConfigRepositoryConfiguration;
    }

    public boolean isUseDesignRepo() {
        return StringUtils.isNotBlank(getDesignRepoForDeployConfig());
    }

    public void setUseDesignRepo(boolean useDesignRepo) {
        if (useDesignRepo) {
            String repo = getDesignRepoForDeployConfig();
            if (StringUtils.isBlank(repo)) {
                String firstRepo = designTimeRepository.getRepositories().get(0).getId();
                properties.setProperty(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG, firstRepo);
            }
        } else {
            properties.setProperty(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG, "");
        }
    }

    public String getDesignRepoForDeployConfig() {
        return properties.getProperty(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG);
    }

    public void setDesignRepoForDeployConfig(String repoId) {
        properties.setProperty(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG, repoId);
    }

    public FolderStructureSettings getFolderStructure(RepositoryConfiguration config) {
        return new FolderStructureSettings(config);
    }

    public FolderStructureSettings getDeployConfigFolderStructure() {
        return new FolderStructureSettings(deployConfigRepositoryConfiguration);
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryEditor.getRepositoryConfigurations();
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

            designRepositoryEditor.validate();
            designRepositoryEditor.save();

            if (!isUseDesignRepo() && !getProductionRepositoryConfigurations().isEmpty()) {
                RepositoryValidators.validate(deployConfigRepositoryConfiguration);
                RepositoryValidators.validateConnection(deployConfigRepositoryConfiguration);
            }

            productionRepositoryEditor.validate();
            productionRepositoryEditor.save(new RepositoryEditor.Callback() {
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
        if (!isUseDesignRepo() && !getProductionRepositoryConfigurations().isEmpty()) {
            deployConfigRepositoryConfiguration.commit();
        }

        // Temporary solution. Made so that when saving settings, if there were no changes, active users are still
        // logged out.
        // This is necessary to reset some parameters such as: information about blocking authentication attempts in
        // git,
        // when the maximum number of attempts is exceeded.
        properties.setProperty("_last.modified.time", Instant.now().toString());

        DynamicPropertySource.get().save(properties.getConfig());

        refreshConfig();
    }

    public void restoreDefaults() {
        try {
            designRepositoryEditor.revertChanges();
            properties.revertProperties(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG);
            deployConfigRepositoryConfiguration.revert();

            productionRepositoryEditor.revertChanges();

            // We cannot invoke configManager.restoreDefaults(): in this case some
            // settings (such as user.mode etc) not edited in this page
            // will be reverted too. We should revert only settings edited in Administration page
            properties
                    .revertProperties(AdministrationSettings.getAllSettings().toArray(StringUtils.EMPTY_STRING_ARRAY));
            saveSystemConfig();
            deployConfigRepositoryConfiguration.revert();
            productionRepositoryEditor.reload();
            designRepositoryEditor.reload();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage(e.getMessage());
        }
    }

    public void addDesignRepository() {
        designRepositoryEditor.addRepository(createRepositoryConfiguration(RepositoryMode.DESIGN));
    }

    public void deleteDesignRepository(String configName) {
        try {
            designRepositoryEditor.deleteRepository(configName);
            designRepositoryAclService.deleteAcl(configName, null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage(e.getMessage());
        }
    }

    public void addProductionRepository() {
        productionRepositoryEditor.addRepository(createRepositoryConfiguration(RepositoryMode.PRODUCTION));
    }

    private RepositoryConfiguration createRepositoryConfiguration(RepositoryMode repositoryMode) {
        List<RepositoryConfiguration> configurations;
        String accessType;

        switch (repositoryMode) {
            case DESIGN:
                accessType = RepositoryType.GIT.factoryId;
                configurations = getDesignRepositoryConfigurations();

                break;
            case PRODUCTION:
                accessType = RepositoryType.DB.factoryId;
                configurations = getProductionRepositoryConfigurations();
                break;
            default:
                throw new IllegalArgumentException("Unsupported repository mode " + repositoryMode);
        }

        String newConfigName = RepositoryEditor.getNewConfigName(configurations, repositoryMode);

        RepositoryConfiguration repoConfig = new RepositoryConfiguration(newConfigName,
                properties,
                accessType,
                configurations,
                repositoryMode);
        repoConfig.commit();
        return repoConfig;
    }

    public void deleteProductionRepository(String configName) {
        try {
            productionRepositoryEditor.deleteRepository(configName, new RepositoryEditor.Callback() {
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
    }

}
