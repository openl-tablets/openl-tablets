package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedProperty;

import org.apache.commons.lang3.StringUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;

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

    @ManagedProperty(value="#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;

    @ManagedProperty(value="#{systemSettingsBean}")
    private SystemSettingsBean systemSettingsBean;

    @ManagedProperty(value="#{productionRepositoryFactoryProxy}")
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    private RepositoryConfiguration defaultRepoConfig;
    private List<RepositoryConfiguration> productionRepositoryConfigurations;

    @PostConstruct
    public void afterPropertiesSet() {
        setProductionRepositoryConfigurations(systemSettingsBean.getProductionRepositoryConfigurations());
        systemSettingsBean = null;
    }

    protected void addProductionRepoToMainConfig(RepositoryConfiguration repoConf) {
        getProductionRepositoryConfigurations().add(repoConf);
    }

    protected List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryConfigurations;
    }

    public void setProductionRepositoryConfigurations(List<RepositoryConfiguration> productionRepositoryConfigurations) {
        this.productionRepositoryConfigurations = productionRepositoryConfigurations;
    }

    public String getConfigurationName(String name) {
        String configName = "rules-";
        if (name != null) {
            configName += name.toLowerCase();
        }
        configName += ".properties";

        return configName;
    }

    protected RepositoryConfiguration createRepositoryConfiguration() {
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(getConfigurationName(getName()), getProductionConfigManager(getName()),
                RepositoryType.PRODUCTION);

        repoConfig.setName(getName());
        repoConfig.setType(getType());
        repoConfig.setPath(getPath());

        if (this.isSecure()) {
            repoConfig.setLogin(getLogin());
            repoConfig.setPassword(getPassword());
        }

        return repoConfig;
    }

    protected RepositoryConfiguration createAdminRepositoryConfiguration() {
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(this.getName(), getProductionConfigManager(getName()),
                RepositoryType.PRODUCTION);

        repoConfig.setName(getName());
        repoConfig.setType(getType());
        repoConfig.setPath(getPath());


        if (this.isSecure()) {
            /*Default Admin credentials for creating new admin user in repo*/
            repoConfig.setLogin("admin");
            repoConfig.setPassword("admin");
        }

        return repoConfig;
    }

    protected RepositoryConfiguration getDefaultRepositoryConfiguration() {
        if (defaultRepoConfig == null) {
            defaultRepoConfig = new RepositoryConfiguration("def", getProductionConfigManager("def"),
                    RepositoryType.PRODUCTION);
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
            RepositoryValidators.validate(prodConfig, getProductionRepositoryConfigurations());

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

    public void setProductionConfigManagerFactory(ConfigurationManagerFactory productionConfigManagerFactory) {
        this.productionConfigManagerFactory = productionConfigManagerFactory;
    }

    protected ConfigurationManager getProductionConfigManager(String name) {
        return productionConfigManagerFactory.getConfigurationManager(getConfigurationName(name));
    }

    public void setSystemSettingsBean(SystemSettingsBean systemSettingsBean) {
        this.systemSettingsBean = systemSettingsBean;
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
