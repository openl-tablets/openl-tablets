package org.openl.rules.webstudio.web.admin;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter.ConfigurationReloader;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.ProductionRepositoriesTreeController;
import org.openl.rules.webstudio.web.servlet.SessionListener;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * TODO Remove property getters/setters when migrating to EL 2.2 TODO Move
 * methods for production repository to another class
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

    private static final Pattern PROHIBITED_CHARACTERS = Pattern.compile("[\\p{Punct}]+");

    private final Log log = LogFactory.getLog(SystemSettingsBean.class);

    private boolean secureDesignRepo = false;

    /** @deprecated */
    private static final BidiMap DESIGN_REPOSITORY_TYPE_FACTORY_MAP = new DualHashBidiMap();
    static {
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("local",
                "org.openl.rules.repository.factories.LocalJackrabbitDesignRepositoryFactory");
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("rmi",
                "org.openl.rules.repository.factories.RmiJackrabbitDesignRepositoryFactory");
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("webdav",
                "org.openl.rules.repository.factories.WebDavJackrabbitDesignRepositoryFactory");
    };
    /** @deprecated */
    private static final Map<String, String> DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP = new HashMap<String, String>();
    static {
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("local", "design-repository.local.home");
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("rmi", "design-repository.remote.rmi.url");
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("webdav", "design-repository.remote.webdav.url");
    };

    /** @deprecated */
    private static final BidiMap PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP = new DualHashBidiMap();
    static {
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("local",
                "org.openl.rules.repository.factories.LocalJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("rmi",
                "org.openl.rules.repository.factories.RmiJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("webdav",
                "org.openl.rules.repository.factories.WebDavJackrabbitProductionRepositoryFactory");
    };
    /** @deprecated */
    private static final Map<String, String> PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP = new HashMap<String, String>();
    static {
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("local", "production-repository.local.home");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("rmi", "production-repository.remote.rmi.url");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("webdav", "production-repository.remote.webdav.url");
    };

    private ConfigurationManager configManager = WebStudioUtils.getWebStudio(true).getSystemConfigManager();

    private List<RepositoryConfiguration> productionRepositoryConfigurations = new ArrayList<RepositoryConfiguration>();
    private List<RepositoryConfiguration> deletedConfigurations = new ArrayList<RepositoryConfiguration>();

    @ManagedProperty(value = "#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;

    @ManagedProperty(value = "#{deploymentManager}")
    private DeploymentManager deploymentManager;

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

    public String getDesignRepositoryType() {
        String factory = configManager.getStringProperty(DESIGN_REPOSITORY_FACTORY);
        return (String) DESIGN_REPOSITORY_TYPE_FACTORY_MAP.getKey(factory);
    }

    public void setDesignRepositoryType(String type) {
        configManager.setProperty(DESIGN_REPOSITORY_FACTORY, DESIGN_REPOSITORY_TYPE_FACTORY_MAP.get(type));
    }

    public String getDesignRepositoryName() {
        return configManager.getStringProperty(DESIGN_REPOSITORY_NAME);
    }

    public void setDesignRepositoryName(String name) {
        configManager.setProperty(DESIGN_REPOSITORY_NAME, name);
    }

    public String getDesignRepositoryPath() {
        String type = getDesignRepositoryType();
        String propName = DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type);

        return "local".equals(type) ? configManager.getPath(propName) : configManager.getStringProperty(propName);
    }

    public void setDesignRepositoryPath(String path) {
        String type = getDesignRepositoryType();
        String propName = DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type);
        String normalizedPath = StringUtils.trimToEmpty(path);

        if ("local".equals(type)) {
            configManager.setPath(propName, normalizedPath);
        } else {
            configManager.setProperty(propName, normalizedPath);
        }
    }

    public boolean isDesignRepositoryPathSystem() {
        String type = getDesignRepositoryType();
        return configManager.isSystemProperty(DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type));
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        if (productionRepositoryConfigurations.isEmpty()) {
            initProductionRepositoryConfigurations();
        }

        return productionRepositoryConfigurations;
    }

    public void setDesignRepositoryLogin(String login) {
        configManager.setProperty(DESIGN_REPOSITORY_LOGIN, login);
    }

    public String getDesignRepositoryLogin() {
        return configManager.getStringProperty(DESIGN_REPOSITORY_LOGIN);
    }

    public void setDesignRepositoryPass(String pass) {
        if (!StringUtils.isEmpty(pass)) {
            configManager.setPassword(DESIGN_REPOSITORY_PASSWORD, pass);
        }
    }

    public String getDesignRepositoryPass() {
        return "";
    }

    public boolean isSecureDesignRepo() {
        return secureDesignRepo || !StringUtils.isEmpty(this.getDesignRepositoryLogin());
    }

    public void setSecureDesignRepo(boolean secureDesignRepo) {
        if (!secureDesignRepo) {
            configManager.removeProperty(DESIGN_REPOSITORY_LOGIN);
            configManager.removeProperty(DESIGN_REPOSITORY_PASSWORD);
            configManager.removeProperty(DESIGN_REPOSITORY_CONFIG_FILE);
        } else {
            configManager.setProperty(DESIGN_REPOSITORY_CONFIG_FILE, RepositoryConfiguration.SECURE_CONFIG_FILE);
        }

        this.secureDesignRepo = secureDesignRepo;
    }

    private void initProductionRepositoryConfigurations() {
        productionRepositoryConfigurations.clear();

        String[] repositoryConfigNames = split(configManager.getStringProperty(PRODUCTION_REPOSITORY_CONFIGS));
        for (String configName : repositoryConfigNames) {
            ConfigurationManager productionConfig = getProductionConfigManager(configName);
            RepositoryConfiguration config = new RepositoryConfiguration(configName, productionConfig);
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
                validate(prodConfig);
                validateConnection(prodConfig);
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
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
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

        for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
            productionRepositoryConfigurations.get(i).delete();
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

    public void addProductionRepository() {
        try {
            String emptyConfigName = "_new_";
            RepositoryConfiguration template = new RepositoryConfiguration(emptyConfigName,
                    getProductionConfigManager(emptyConfigName));

            String templateName = template.getName();
            String[] configNames = split(configManager.getStringProperty(PRODUCTION_REPOSITORY_CONFIGS));
            long maxNumber = getMaxTemplatedConfigName(configNames, templateName);

            String templatePath = template.getPath();
            String[] paths = new String[productionRepositoryConfigurations.size()];
            for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
                paths[i] = productionRepositoryConfigurations.get(i).getPath();
            }

            String newNum = String.valueOf(maxNumber + 1);
            String newConfigName = getConfigName(templateName + newNum);
            RepositoryConfiguration newConfig = new RepositoryConfiguration(newConfigName,
                    getProductionConfigManager(newConfigName));
            newConfig.setName(templateName + newNum);
            newConfig.setPath(templatePath + (getMaxTemplatedPath(paths, templatePath) + 1));

            configNames = (String[]) ArrayUtils.add(configNames, newConfigName);

            productionRepositoryConfigurations.add(newConfig);
            // FacesUtils.addInfoMessage("Repository '" + newConfig.getName() +
            // "' is added successfully");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            FacesUtils.addErrorMessage(e.getMessage());
        }
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
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    public void saveProductionRepository(String configName) {
        for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
            RepositoryConfiguration prodConfig = productionRepositoryConfigurations.get(i);
            if (prodConfig.getConfigName().equals(configName)) {
                try {
                    validate(prodConfig);

                    productionRepositoryConfigurations.set(i, saveProductionRepository(prodConfig));
                    FacesUtils.addInfoMessage("Repository '" + prodConfig.getName() + "' is saved successfully");
                } catch (Exception e) {
                    FacesUtils.addErrorMessage(e.getMessage());
                }
                break;
            }
        }
    }

    public void validate(RepositoryConfiguration prodConfig) throws RepositoryValidationException {
        if (StringUtils.isEmpty(prodConfig.getName())) {
            String msg = String.format("Repository name is empty. Please, enter repository name", prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }
        if (StringUtils.isEmpty(prodConfig.getPath())) {
            String msg = String.format("Repository path is empty. Please, enter repository path", prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }

        if (PROHIBITED_CHARACTERS.matcher(prodConfig.getName()).find()) {
            String msg = String.format(
                    "Repository name '%s' contains illegal characters. Please, correct repository name",
                    prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }

        // workingDirValidator(prodConfig.getPath(),
        // "Production Repository directory");

        // Check for name uniqueness.
        for (RepositoryConfiguration other : productionRepositoryConfigurations) {
            if (other != prodConfig) {
                if (prodConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one",
                            prodConfig.getName());
                    throw new RepositoryValidationException(msg);
                }

                if (prodConfig.getPath().equals(other.getPath())) {
                    String msg = String.format("Repository path '%s' already exists. Please, insert a new one",
                            prodConfig.getPath());
                    throw new RepositoryValidationException(msg);
                }
            }
        }
    }

    /*FIXME move to utils class*/
    public void validateConnection(RepositoryConfiguration repoConfig) throws RepositoryValidationException {
        try {
            /**Close connection to jcr before checking connection*/
            this.getProductionRepositoryFactoryProxy().releaseRepository(repoConfig.getConfigName());
            RRepositoryFactory repoFactory = this.getProductionRepositoryFactoryProxy().getFactory(
                    repoConfig.getProperties());
            repoFactory = this.getProductionRepositoryFactoryProxy().getFactory(
                    repoConfig.getProperties());

            RRepository repository = repoFactory.getRepositoryInstance();
            /*Close repo connection after validation*/
            repository.release();
            this.getProductionRepositoryFactoryProxy().releaseRepository(repoConfig.getConfigName());
        } catch (RRepositoryException e) {
            Throwable resultException = e;

            while (resultException.getCause() != null) {
                resultException = resultException.getCause();
            }

            if (resultException instanceof javax.jcr.LoginException) {
                if (!repoConfig.isSecure()) {
                    throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                            + "\" : Connection is secure. Please, insert login and password");
                } else {
                    throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                            + "\" : Invalid login or password. Please, check login and password");
                }
            } else if (resultException instanceof javax.security.auth.login.FailedLoginException) {
                throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                        + "\" : Invalid login or password. Please, check login and password");
            } else if (resultException instanceof java.net.ConnectException) {
                throw new RepositoryValidationException("Connection refused. Please, check repository URL");
            }

            throw new RepositoryValidationException("Repository \"" + repoConfig.getName() + "\" : "
                    + resultException.getMessage());
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
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }

            deploymentManager.addRepository(newConfigName);
        }
        return prodConfig;
    }

    private RepositoryConfiguration renameConfigName(RepositoryConfiguration prodConfig) throws ServletException {
        // Move config to a new file
        String newConfigName = getConfigName(prodConfig.getName());
        RepositoryConfiguration newConfig = new RepositoryConfiguration(newConfigName,
                getProductionConfigManager(newConfigName));
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

    private long getMaxTemplatedConfigName(String[] configNames, String templateName) {
        return getMaxNumberOfTemplatedNames(configNames, templateName, "rules-", ".properties");
    }

    private long getMaxTemplatedPath(String[] configNames, String templateName) {
        return getMaxNumberOfTemplatedNames(configNames, templateName, "", "");
    }

    private long getMaxNumberOfTemplatedNames(String[] configNames, String templateName, String prefix, String suffix) {
        Pattern pattern = Pattern.compile("\\Q" + prefix + templateName + "\\E\\d+\\Q" + suffix + "\\E",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        int startPosition = (prefix + templateName).length();
        int suffixLength = suffix.length();

        long maxNumber = 0;
        for (String configName : configNames) {
            if (pattern.matcher(configName).matches()) {
                long sequenceNumber;
                try {
                    sequenceNumber = Long.parseLong(configName.substring(startPosition, configName.length()
                            - suffixLength));
                } catch (NumberFormatException ignore) {
                    continue;
                }

                if (sequenceNumber > maxNumber) {
                    maxNumber = sequenceNumber;
                }
            }
        }
        return maxNumber;
    }

    public void dateFormatValidator(FacesContext context, UIComponent toValidate, Object value) {
        String inputDate = (String) value;

        validateNotBlank(inputDate, "Date format");

    }

    public void workSpaceDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        String directoryType = "Workspace Directory";
        validateNotBlank((String) value, directoryType);
        setUserWorkspaceHome((String) value);
        workingDirValidator(getUserWorkspaceHome(), directoryType);

    }

    public void historyDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        String directoryType = "History Directory";
        validateNotBlank((String) value, directoryType);
        setProjectHistoryHome((String) value);
        workingDirValidator(getProjectHistoryHome(), directoryType);

    }

    public void historyCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String errorMessage = null;
        String count = (String) value;
        validateNotBlank(count, "The maximum count of saved changes");
        if (!Pattern.matches("[0-9]+", count)) {
            errorMessage = "The maximum count of saved changes should be positive integer";
        }

        if (errorMessage != null) {
            FacesUtils.addErrorMessage(errorMessage);
            throw new ValidatorException(new FacesMessage(errorMessage));
        }
    }

    public void designRepositoryValidator(FacesContext context, UIComponent toValidate, Object value) {
        String directoryType = "Design Repository directory";
        validateNotBlank((String) value, directoryType);
        setDesignRepositoryPath((String) value);
        workingDirValidator(getDesignRepositoryPath(), directoryType);
    }

    public void productionRepositoryValidator(FacesContext context, UIComponent toValidate, Object value) {
        String directoryType = "Production Repositories directory";
        validateNotBlank((String) value, directoryType);
        workingDirValidator((String) value, directoryType);
    }

    public void maxCachedProjectsCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String count = (String) value;
        validateNotNegativeInteger(count, "The maximum number of cached projects");
    }

    public void cachedProjectIdleTimeValidator(FacesContext context, UIComponent toValidate, Object value) {
        String count = (String) value;
        validateNotNegativeInteger(count, "The time to store a project in cache");
    }

    public void testRunThreadCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String count = (String) value;
        validateGreaterThanZero(count, "Number of threads");
    }

    private void validateNotNegativeInteger(String count, String target) {
        String message = target + " must be positive integer or zero";
        try {
            int v = Integer.parseInt(StringUtils.trim(count));
            if (v < 0) {
                FacesUtils.addErrorMessage(message);
                throw new ValidatorException(new FacesMessage(message));
            }
        } catch (NumberFormatException e) {
            FacesUtils.addErrorMessage(message);
            throw new ValidatorException(new FacesMessage(message));
        }
    }

    private void validateGreaterThanZero(String count, String target) {
        String message = target + " must be positive integer";
        try {
            int v = Integer.parseInt(StringUtils.trim(count));
            if (v <= 0) {
                FacesUtils.addErrorMessage(message);
                throw new ValidatorException(new FacesMessage(message));
            }
        } catch (NumberFormatException e) {
            FacesUtils.addErrorMessage(message);
            throw new ValidatorException(new FacesMessage(message));
        }
    }

    public void workingDirValidator(String value, String folderType) {
        File studioWorkingDir;
        File tmpFile = null;
        boolean hasAccess;

        try {

            studioWorkingDir = new File(value);

            if (studioWorkingDir.exists()) {
                tmpFile = new File(studioWorkingDir.getAbsolutePath() + File.separator + "tmp");

                hasAccess = tmpFile.mkdir();

                if (!hasAccess) {
                    throw new ValidatorException(new FacesMessage("Can't get access to the folder ' " + value
                            + " '    Please, contact to your system administrator."));
                }
            } else {
                if (!studioWorkingDir.mkdirs()) {
                    throw new ValidatorException(new FacesMessage("Incorrect " + folderType + " '" + value + "'"));
                } else {
                    deleteFolder(value);
                }
            }
        } catch (Exception e) {
            FacesUtils.addErrorMessage(e.getMessage());
            throw new ValidatorException(new FacesMessage(e.getMessage()));

        } finally {
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    private void validateNotBlank(String value, String folderType) throws ValidatorException {
        if (StringUtils.isBlank(value)) {
            String errorMessage = folderType + " could not be empty";
            FacesUtils.addErrorMessage(errorMessage);
            throw new ValidatorException(new FacesMessage(errorMessage));
        }
    }

    /* Deleting the folder which were created for validating folder permissions */
    private void deleteFolder(String folderPath) {
        File workFolder = new File(folderPath);
        File parent = workFolder.getParentFile();

        while (parent != null) {
            workFolder.delete();
            parent = workFolder.getParentFile();
            workFolder = parent;
        }
    }

    public ProductionRepositoriesTreeController getProductionRepositoriesTreeController() {
        return productionRepositoriesTreeController;
    }

    public void setProductionRepositoriesTreeController(
            ProductionRepositoriesTreeController productionRepositoriesTreeController) {
        this.productionRepositoriesTreeController = productionRepositoriesTreeController;
    }

    public ProductionRepositoryFactoryProxy getProductionRepositoryFactoryProxy() {
        return productionRepositoryFactoryProxy;
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
