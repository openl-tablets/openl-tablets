/**
 * 
 */
package org.openl.rules.webstudio.web.admin;

import javax.faces.bean.ManagedProperty;

import org.apache.commons.lang3.StringUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * @author Pavel Tarasevich
 * 
 */

public abstract class AbstractProductionRepoController {
    private static final String LOCAL = "local";
    private String name;
    private String type = LOCAL;
    private String path;
    private boolean secure = false;
    private String login;
    private String password;
    private boolean checked = false;
    private String errorMessage = "";

    private ConfigurationManager configManager = WebStudioUtils.getWebStudio(true).getSystemConfigManager();
    @ManagedProperty(value="#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;

    @ManagedProperty(value="#{systemSettingsBean}")
    private SystemSettingsBean systemSettingsBean;

    @ManagedProperty(value="#{productionRepositoryFactoryProxy}")
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    private String secureConfiguration = RepositoryConfiguration.SECURE_CONFIG_FILE;
    private RepositoryConfiguration defaultRepoConfig;

    protected void addProductionRepoToMainConfig(RepositoryConfiguration repoConf) {
        systemSettingsBean.getProductionRepositoryConfigurations().add(repoConf);
    }

    public String getConfigurationName(String name) {
        String configName = "rules-";
        if (name != null) {
            configName += name.toLowerCase();
        }
        configName += ".properties";

        return configName;
    }

    protected RepositoryConfiguration createRepositoryConfiguration(String connectionType) {
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(getConfigurationName(this.getName()), getProductionConfigManager(getName()));

        repoConfig.setName(getName());
        repoConfig.setType(getType());
        repoConfig.setPath(getPath());

        if (this.isSecure()) {
            repoConfig.setLogin(getLogin());
            repoConfig.setPassword(getPassword());

            repoConfig.setConfigFile(this.getSecureConfiguration());
        }

        repoConfig.setConnectionType(connectionType);

        return repoConfig;
    }

    protected RepositoryConfiguration createAdminRepositoryConfiguration(String connectionType) {
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(this.getName(), getProductionConfigManager(getName()));

        repoConfig.setName(getName());
        repoConfig.setType(getType());
        repoConfig.setPath(getPath());


        if (this.isSecure()) {
            /*Default Admin credencials for creating new admin user in repo*/
            repoConfig.setLogin("admin");
            repoConfig.setPassword("admin");

            repoConfig.setConfigFile(this.getSecureConfiguration());
        }

        repoConfig.setConnectionType(connectionType);

        return repoConfig;
    }

    protected RepositoryConfiguration getDefaultRepositoryConfiguration() {
        if (defaultRepoConfig == null) {
            defaultRepoConfig = new RepositoryConfiguration("def", getProductionConfigManager("def"));
        }
        
        return defaultRepoConfig;
    }

    public void clearForm() {
        name = "";
        type = LOCAL;
        path = "";
        secure = false;
        login = "";
        password = "";
        checked = false;
        errorMessage = "";
    }

    public boolean isInputParamValid(RepositoryConfiguration prodConfig) {
        try {
            systemSettingsBean.validate(prodConfig);

            if (this.secure) {
                if (StringUtils.isEmpty(this.login) || StringUtils.isEmpty(this.password)) {
                    throw new RepositoryValidationException("Invalid login or password. Please, check login and password");
                }
            }

            return true;
        } catch (RepositoryValidationException e) {
            this.errorMessage = e.getMessage();
            return false;
        }
    }

    public abstract void save();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        if (StringUtils.isEmpty(path)) {
            this.getDefaultRepositoryConfiguration().setType(this.getType());
            return this.getDefaultRepositoryConfiguration().getPath();
        }

        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    public void setConfigManager(ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    public ConfigurationManagerFactory getProductionConfigManagerFactory() {
        return productionConfigManagerFactory;
    }

    public void setProductionConfigManagerFactory(ConfigurationManagerFactory productionConfigManagerFactory) {
        this.productionConfigManagerFactory = productionConfigManagerFactory;
    }

    protected ConfigurationManager getProductionConfigManager(String name) {
        return productionConfigManagerFactory.getConfigurationManager(getConfigurationName(name));
    }

    public SystemSettingsBean getSystemSettingsBean() {
        return systemSettingsBean;
    }

    public void setSystemSettingsBean(SystemSettingsBean systemSettingsBean) {
        this.systemSettingsBean = systemSettingsBean;
    }

    public String getSecureConfiguration() {
        return secureConfiguration;
    }

    public void setSecureConfiguration(String secureConfiguration) {
        this.secureConfiguration = secureConfiguration;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public ProductionRepositoryFactoryProxy getProductionRepositoryFactoryProxy() {
        return productionRepositoryFactoryProxy;
    }

    public void setProductionRepositoryFactoryProxy(ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isLocal() {
        return LOCAL.equals(getType());
    }
}
