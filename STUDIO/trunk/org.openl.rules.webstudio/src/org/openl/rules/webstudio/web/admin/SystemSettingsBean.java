package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * TODO Remove property getters/setters when migrating to EL 2.2
 * TODO Move methods for production repository to another class
 * 
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class SystemSettingsBean {
    private final Log log = LogFactory.getLog(SystemSettingsBean.class);

    private static final String WORKSPACES_ROOT = "workspace.root";
    private static final String LOCAL_WORKSPACE = "workspace.local.home";
    private static final String PROJECT_HISTORY_HOME = "project.history.home";
    private static final String DATE_PATTERN = "data.format.date";

    private static final String AUTO_LOGIN = "security.login.auto";

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
    
    @ManagedProperty(value="#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;
    
    @ManagedProperty(value="#{deploymentManager}")
    private DeploymentManager deploymentManager;

    public String getWorkspacesRoot() {
        return configManager.getStringProperty(WORKSPACES_ROOT);
    }

    public void setWorkspacesRoot(String workspacesRoot) {
        configManager.setProperty(WORKSPACES_ROOT, workspacesRoot);
    }

    public String getLocalWorkspace() {
        return configManager.getStringProperty(LOCAL_WORKSPACE);
    }

    public void setLocalWorkspace(String localWorkspace) {
        configManager.setProperty(LOCAL_WORKSPACE, localWorkspace);
    }

    public String getDatePattern() {
        return configManager.getStringProperty(DATE_PATTERN);
    }

    public void setDatePattern(String datePattern) {
        configManager.setProperty(DATE_PATTERN, datePattern);
    }

    public boolean isAutoLogin() {
        return configManager.getBooleanProperty(AUTO_LOGIN);
    }

    public void setAutoLogin(boolean autoLogin) {
        configManager.setProperty(AUTO_LOGIN, autoLogin);
    }

    public String getProjectHistoryHome() {
        return configManager.getStringProperty(PROJECT_HISTORY_HOME);
    }

    public void setProjectHistoryHome(String projectHistoryHome) {
        configManager.setProperty(PROJECT_HISTORY_HOME, projectHistoryHome);
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
        return configManager.getStringProperty(
                DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type));
    }

    public void setDesignRepositoryPath(String path) {
        String type = getDesignRepositoryType();
        configManager.setProperty(
                DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type), path);
    }

    public boolean isDesignRepositoryPathSystem() {
        String type = getDesignRepositoryType();
        return configManager.isSystemProperty(
                DESIGN_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type));
    }
    
    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        productionRepositoryConfigurations.clear();
        
        String[] repositoryConfigNames = configManager.getStringArrayProperty(PRODUCTION_REPOSITORY_CONFIGS);
        for (String configName : repositoryConfigNames) {
            ConfigurationManager productionConfig = getProductionConfigManager(configName);
            RepositoryConfiguration config = new RepositoryConfiguration(configName, productionConfig);
            productionRepositoryConfigurations.add(config);
        }
        
        return productionRepositoryConfigurations;
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
    
            for (RepositoryConfiguration prodConfig : productionRepositoryConfigurations) {
                saveProductionRepository(prodConfig);
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
        boolean restored = configManager.restoreDefaults();
        // TODO remove production repository properties
        if (restored) {
            WebStudioUtils.getWebStudio().setNeedRestart(true);
        }
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
            String[] paths = new String[configNames.length];
            for (int i = 0; i < configNames.length; i++) {
                paths[i] = new RepositoryConfiguration(configNames[i], getProductionConfigManager(configNames[i])).getPath();
            }
            
            String newNum = String.valueOf(maxNumber + 1);
            String newConfignName = getConfigName(templateName + newNum);
            RepositoryConfiguration newConfig = new RepositoryConfiguration(newConfignName, getProductionConfigManager(newConfignName));
            newConfig.setName(templateName + newNum);
            newConfig.setPath(templatePath + (getMaxTemplatedPath(paths, templatePath) + 1));
            newConfig.save();
            
            configNames = (String[]) ArrayUtils.add(configNames, newConfignName);
            configManager.setProperty(PRODUCTION_REPOSITORY_CONFIGS, configNames);
            saveSystemConfig();
            
            deploymentManager.addRepository(newConfignName);
            FacesUtils.addInfoMessage("Repository '" + newConfig.getName() + "' is added successfully");
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
            
            String repositoryName = "";
            for (RepositoryConfiguration prodConfig : productionRepositoryConfigurations) {
                if (prodConfig.getConfigName().equals(configName)) {
                    repositoryName = prodConfig.getName();
                    prodConfig.delete();
                    break;
                }
            }
    
            saveSystemConfig();
            FacesUtils.addInfoMessage("Repository '" + repositoryName + "' is deleted successfully");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }
    
    public void saveProductionRepository(String configName) {
        for (RepositoryConfiguration prodConfig : productionRepositoryConfigurations) {
            if (prodConfig.getConfigName().equals(configName)) {
                try {
                    validate(prodConfig);
    
                    saveProductionRepository(prodConfig);
                    FacesUtils.addInfoMessage("Repository '" + prodConfig.getName() + "' is saved successfully");
                    break;
                } catch (Exception e) {
                    FacesUtils.addErrorMessage(e.getMessage());
                }
            }
        }
    }

    private void validate(RepositoryConfiguration prodConfig) throws RepositoryValidationException {
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

    private void saveProductionRepository(RepositoryConfiguration prodConfig) {
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
        Pattern pattern = Pattern.compile("\\Q"+ prefix + templateName.toLowerCase() + "\\E\\d+\\Q" + suffix + "\\E");
        
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
}
