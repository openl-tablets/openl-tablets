package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedProperty;

import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.util.StringUtils;

/**
 * @author Pavel Tarasevich
 * 
 */
public abstract class AbstractProductionRepoController {
    private RepositoryConfiguration repositoryConfiguration;

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
        repositoryConfiguration = createDummyRepositoryConfiguration();
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

    private String getConfigurationName(String name) {
        String configName = "rules-";
        if (name != null) {
            configName += name.toLowerCase();
        }
        configName += ".properties";

        return configName;
    }

    public RepositoryConfiguration getRepositoryConfiguration() {
        return repositoryConfiguration;
    }

    protected RepositoryConfiguration createRepositoryConfiguration() {
        String name = repositoryConfiguration.getName();
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(getConfigurationName(name), getProductionConfigManager(name),
                RepositoryMode.PRODUCTION);

        repoConfig.copyContent(repositoryConfiguration);
        repoConfig.commit();
        return repoConfig;
    }

    protected RepositoryConfiguration createAdminRepositoryConfiguration() {
        String name = repositoryConfiguration.getName();
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(name, getProductionConfigManager(name),
                RepositoryMode.PRODUCTION);

        repoConfig.copyContent(repositoryConfiguration);

        RepositorySettings settings = repoConfig.getSettings();
        if (settings instanceof CommonRepositorySettings) {
            CommonRepositorySettings repoSettings = (CommonRepositorySettings) settings;

            if (repoSettings.isSecure()) {
            /*Default Admin credentials for creating new admin user in repo*/
                repoSettings.setLogin("admin");
                repoSettings.setPassword("admin");
            }
        }

        return repoConfig;
    }

    public void clearForm() {
        repositoryConfiguration = createDummyRepositoryConfiguration();
        errorMessage = "";
    }

    private RepositoryConfiguration createDummyRepositoryConfiguration() {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration("def", getProductionConfigManager("def"),
                RepositoryMode.PRODUCTION);
        repositoryConfiguration.setType(RepositoryType.LOCAL.name().toLowerCase());
        return repositoryConfiguration;
    }

    public boolean isInputParamInvalid(RepositoryConfiguration prodConfig) {
        try {
            RepositoryValidators.validate(prodConfig, getProductionRepositoryConfigurations());

            RepositorySettings settings = repositoryConfiguration.getSettings();
            if (settings instanceof CommonRepositorySettings) {
                CommonRepositorySettings s = (CommonRepositorySettings) settings;
                if (s.isSecure()) {
                    if (StringUtils.isEmpty(s.getLogin()) || StringUtils.isEmpty(s.getPassword())) {
                        throw new RepositoryValidationException("Invalid login or password. Please, check login and password");
                    }
                }
            }

            return false;
        } catch (RepositoryValidationException e) {
            this.errorMessage = e.getMessage();
            return true;
        }
    }

    public abstract void save();

    public void setProductionConfigManagerFactory(ConfigurationManagerFactory productionConfigManagerFactory) {
        this.productionConfigManagerFactory = productionConfigManagerFactory;
    }

    private ConfigurationManager getProductionConfigManager(String name) {
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
}
