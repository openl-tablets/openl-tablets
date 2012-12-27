package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

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
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.ProductionRepositoriesTreeController;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * TODO Remove property getters/setters when migrating to EL 2.2
 * TODO Move methods for production repository to another class
 * 
 * @author Andrei Astrouski
 */
@ManagedBean
@SessionScoped
public class SystemSettingsBean {
    @ManagedProperty(value="#{productionRepositoriesTreeController}")
    private ProductionRepositoriesTreeController productionRepositoriesTreeController;
    
    private static final Pattern PROHIBITED_CHARACTERS = Pattern.compile("[\\p{Punct}]+");

    private final Log log = LogFactory.getLog(SystemSettingsBean.class);

    private static final String USER_WORKSPACE_HOME = "user.workspace.home";
    private static final String PROJECT_HISTORY_HOME = "project.history.home";
    private static final String DATE_PATTERN = "data.format.date";
    public static final String UPDATE_SYSTEM_PROPERTIES = "update.system.properties";

    private static final String DESIGN_REPOSITORY_FACTORY = "design-repository.factory";
    private static final String DESIGN_REPOSITORY_NAME = "design-repository.name";
    /** @deprecated */
    private static final BidiMap DESIGN_REPOSITORY_TYPE_FACTORY_MAP = new DualHashBidiMap();
    static {
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("local", "org.openl.rules.repository.factories.LocalJackrabbitDesignRepositoryFactory");
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("rmi", "org.openl.rules.repository.factories.RmiJackrabbitDesignRepositoryFactory");
        DESIGN_REPOSITORY_TYPE_FACTORY_MAP.put("webdav", "org.openl.rules.repository.factories.WebDavJackrabbitDesignRepositoryFactory");
    };
    /** @deprecated */
    private static final Map<String, String> DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP = new HashMap<String, String>();
    static {
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("local", "design-repository.local.home");
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("rmi", "design-repository.remote.rmi.url");
        DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("webdav", "design-repository.remote.webdav.url");
    };

    private static final String PRODUCTION_REPOSITORY_CONFIGS = "production-repository-configs";
    /** @deprecated */
    private static final BidiMap PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP = new DualHashBidiMap();
    static {
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("local", "org.openl.rules.repository.factories.LocalJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("rmi", "org.openl.rules.repository.factories.RmiJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("webdav", "org.openl.rules.repository.factories.WebDavJackrabbitProductionRepositoryFactory");
    };
    /** @deprecated */
    private static final Map<String, String> PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP = new HashMap<String, String>();
    static {
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("local", "production-repository.local.home");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("rmi", "production-repository.remote.rmi.url");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("webdav", "production-repository.remote.webdav.url");
    };

    private ConfigurationManager configManager = WebStudioUtils.getWebStudio().getSystemConfigManager();

    private List<RepositoryConfiguration> productionRepositoryConfigurations = new ArrayList<RepositoryConfiguration>();
    private List<RepositoryConfiguration> deletedConfigurations = new ArrayList<RepositoryConfiguration>();
    
    @ManagedProperty(value="#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;

    @ManagedProperty(value="#{deploymentManager}")
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

    public String getDesignRepositoryType() {
        String factory = configManager.getStringProperty(DESIGN_REPOSITORY_FACTORY);
        return (String) DESIGN_REPOSITORY_TYPE_FACTORY_MAP.getKey(factory);
    }

    public void setDesignRepositoryType(String type) {
        configManager.setProperty(
                DESIGN_REPOSITORY_FACTORY, DESIGN_REPOSITORY_TYPE_FACTORY_MAP.get(type));
    }

    public String getDesignRepositoryName() {
        return configManager.getStringProperty(DESIGN_REPOSITORY_NAME);
    }

    public void setDesignRepositoryName(String name) {
        configManager.setProperty(DESIGN_REPOSITORY_NAME, name);
    }

    public String getDesignRepositoryPath() {
        String type = getDesignRepositoryType();
        return configManager.getPath(DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type));
    }

    public void setDesignRepositoryPath(String path) {
        String type = getDesignRepositoryType();
        configManager.setPath(DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type), path);
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

    private void initProductionRepositoryConfigurations() {
        productionRepositoryConfigurations.clear();

        String[] repositoryConfigNames = configManager.getStringArrayProperty(PRODUCTION_REPOSITORY_CONFIGS);
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
    
    public String getRulesDispatchingMode() {
        return OpenLSystemProperties.getDispatchingMode(configManager.getProperties());
    }

    public void setRulesDispatchingMode(String dispatchingMode) {
        configManager.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, dispatchingMode);
    }

    public void applyChanges() {
        try {
            for (RepositoryConfiguration prodConfig : productionRepositoryConfigurations) {
                validate(prodConfig);
            }
            
            for (RepositoryConfiguration prodConfig : deletedConfigurations) {
                prodConfig.delete();
            }
            deletedConfigurations.clear();
    
            for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
                RepositoryConfiguration prodConfig = productionRepositoryConfigurations.get(i);
                productionRepositoryConfigurations.set(i, saveProductionRepository(prodConfig));
            }
    
            saveSystemConfig();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    private void saveSystemConfig() {
        boolean saved = configManager.save();
        if (saved) {
            WebStudioUtils.getWebStudio().setNeedRestart(true);
        }
    }

    public void restoreDefaults() {
        for (RepositoryConfiguration prodConfig : deletedConfigurations) {
            prodConfig.delete();
        }
        deletedConfigurations.clear();

        for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
            productionRepositoryConfigurations.get(i).delete();
        }
        productionRepositoryConfigurations.clear();

        boolean restored = configManager.restoreDefaults();
        if (restored) {
            WebStudioUtils.getWebStudio().setNeedRestart(true);
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
            RepositoryConfiguration template = new RepositoryConfiguration(emptyConfigName, getProductionConfigManager(emptyConfigName));
            
            String templateName = template.getName();
            String[] configNames = configManager.getStringArrayProperty(PRODUCTION_REPOSITORY_CONFIGS);
            long maxNumber = getMaxTemplatedConfigName(configNames, templateName);

            String templatePath = template.getPath();
            String[] paths = new String[productionRepositoryConfigurations.size()];
            for (int i = 0; i < productionRepositoryConfigurations.size(); i++) {
                paths[i] = productionRepositoryConfigurations.get(i).getPath();
            }

            String newNum = String.valueOf(maxNumber + 1);
            String newConfigName = getConfigName(templateName + newNum);
            RepositoryConfiguration newConfig = new RepositoryConfiguration(newConfigName, getProductionConfigManager(newConfigName));
            newConfig.setName(templateName + newNum);
            newConfig.setPath(templatePath + (getMaxTemplatedPath(paths, templatePath) + 1));

            configNames = (String[]) ArrayUtils.add(configNames, newConfigName);
            configManager.setProperty(PRODUCTION_REPOSITORY_CONFIGS, configNames);

            productionRepositoryConfigurations.add(newConfig);
//            FacesUtils.addInfoMessage("Repository '" + newConfig.getName() + "' is added successfully");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    public void deleteProductionRepository(String configName) {
        try {
            deploymentManager.removeRepository(configName);
            
            String[] configNames = configManager.getStringArrayProperty(PRODUCTION_REPOSITORY_CONFIGS);
            configNames = (String[]) ArrayUtils.removeElement(configNames, configName);
            configManager.setProperty(PRODUCTION_REPOSITORY_CONFIGS, configNames);
            
            Iterator<RepositoryConfiguration> it = productionRepositoryConfigurations.iterator();
            while (it.hasNext()) {
                RepositoryConfiguration prodConfig = it.next();
                if (prodConfig.getConfigName().equals(configName)) {
                    deletedConfigurations.add(prodConfig);
                    it.remove();
                    /*Delete Production repo from tree*/
                    productionRepositoriesTreeController.deleteProdRepo(prodConfig.getName());
                    break;
                }
            }

//            FacesUtils.addInfoMessage("Repository '" + repositoryName + "' is deleted successfully");
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

    private void validate(RepositoryConfiguration prodConfig) throws RepositoryValidationException {
        if (StringUtils.isEmpty(prodConfig.getName())) {
            String msg = String.format("Repository name is empty", prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }
        if (StringUtils.isEmpty(prodConfig.getPath())) {
            String msg = String.format("Repository path is empty", prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }
        if (PROHIBITED_CHARACTERS.matcher(prodConfig.getName()).find()) {
            String msg = String.format("Repository name '%s' contains illegal characters", prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }
        // Check for name uniqueness.
        for (RepositoryConfiguration other : productionRepositoryConfigurations) {
            if (other != prodConfig) {
                if (prodConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists", prodConfig.getName());
                    throw new RepositoryValidationException(msg);
                }

                if (prodConfig.getPath().equals(other.getPath())) {
                    String msg = String.format("Repository path '%s' already exists", prodConfig.getPath());
                    throw new RepositoryValidationException(msg);
                }
            }
        }
    }

    private RepositoryConfiguration saveProductionRepository(RepositoryConfiguration prodConfig) {
        boolean changed = prodConfig.save();
        if (changed) {
            try {
                deploymentManager.removeRepository(prodConfig.getConfigName());
            } catch (RRepositoryException e) {
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }

            if (prodConfig.isNameChangedIgnoreCase()) {
                prodConfig = renameConfigName(prodConfig);
            }

            deploymentManager.addRepository(prodConfig.getConfigName());
        }
        return prodConfig;
    }

    private RepositoryConfiguration renameConfigName(RepositoryConfiguration prodConfig) {
        // Move config to a new file
        String newConfigName = getConfigName(prodConfig.getName());
        RepositoryConfiguration newConfig = new RepositoryConfiguration(newConfigName, getProductionConfigManager(newConfigName));
        newConfig.copyContent(prodConfig);
        newConfig.save();
        
        // Rename link to a file in system config
        String[] configNames = configManager.getStringArrayProperty(PRODUCTION_REPOSITORY_CONFIGS);
        for (int i = 0; i < configNames.length; i++) {
            if (configNames[i].equals(prodConfig.getConfigName())) {
                // Found necessary link - rename it
                configNames[i] = newConfigName;
                configManager.setProperty(PRODUCTION_REPOSITORY_CONFIGS, configNames);
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
        Pattern pattern = Pattern.compile("\\Q"+ prefix + templateName + "\\E\\d+\\Q" + suffix + "\\E", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        
        int startPosition = (prefix + templateName).length();
        int suffixLength = suffix.length();
        
        long maxNumber = 0;
        for (String configName : configNames) {
            if (pattern.matcher(configName).matches()) {
                long sequenceNumber;
                try {
                    sequenceNumber = Long.parseLong(configName.substring(startPosition, configName.length() - suffixLength));
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

    public ProductionRepositoriesTreeController getProductionRepositoriesTreeController() {
        return productionRepositoriesTreeController;
    }

    public void setProductionRepositoriesTreeController(
            ProductionRepositoriesTreeController productionRepositoriesTreeController) {
        this.productionRepositoriesTreeController = productionRepositoriesTreeController;
    }
    
    
}
