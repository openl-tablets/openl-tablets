package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedProperty;

import org.openl.config.PropertiesHolder;
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

    @ManagedProperty(value = "#{systemSettingsBean}")
    private SystemSettingsBean systemSettingsBean;

    @ManagedProperty(value = "#{productionRepositoryFactoryProxy}")
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    private PropertiesHolder properties;

    private List<RepositoryConfiguration> productionRepositoryConfigurations;

    @PostConstruct
    public void afterPropertiesSet() {
        setProductionRepositoryConfigurations(systemSettingsBean.getProductionRepositoryConfigurations());
        setProperties(systemSettingsBean.getProperties());
        repositoryConfiguration = createDummyRepositoryConfiguration();
        systemSettingsBean = null;
    }

    protected void addProductionRepoToMainConfig(RepositoryConfiguration repoConf) {
        getProductionRepositoryConfigurations().add(repoConf);
    }

    protected List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryConfigurations;
    }

    public void setProductionRepositoryConfigurations(
            List<RepositoryConfiguration> productionRepositoryConfigurations) {
        this.productionRepositoryConfigurations = productionRepositoryConfigurations;
    }

    public void setProperties(PropertiesHolder properties) {
        this.properties = ProductionRepositoryEditor.createProductionPropertiesWrapper(properties);
    }

    public RepositoryConfiguration getRepositoryConfiguration() {
        return repositoryConfiguration;
    }

    protected RepositoryConfiguration createRepositoryConfiguration() {
        String name = repositoryConfiguration.getName();
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(name, properties);

        repoConfig.copyContent(repositoryConfiguration);
        repoConfig.commit();
        return repoConfig;
    }

    public void clearForm() {
        repositoryConfiguration = createDummyRepositoryConfiguration();
        errorMessage = "";
    }

    private RepositoryConfiguration createDummyRepositoryConfiguration() {
        RepositoryConfiguration previousConfig = productionRepositoryConfigurations.get(0);
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration(previousConfig.getConfigName(),
            properties);
        repositoryConfiguration.setType(RepositoryType.DB.name().toLowerCase());
        return repositoryConfiguration;
    }

    public boolean isInputParamInvalid(RepositoryConfiguration prodConfig) {
        try {
            RepositoryValidators.validate(prodConfig, getProductionRepositoryConfigurations());

            RepositorySettings settings = repositoryConfiguration.getSettings();
            if (settings instanceof CommonRepositorySettings) {
                CommonRepositorySettings s = (CommonRepositorySettings) settings;
                if (s.isSecure() && (StringUtils.isEmpty(s.getLogin()) || StringUtils.isEmpty(s.getPassword()))) {
                    throw new RepositoryValidationException(
                        "Invalid login or password. Please, check login and password");
                }
            }

            return false;
        } catch (RepositoryValidationException e) {
            this.errorMessage = e.getMessage();
            return true;
        }
    }

    public abstract void save();

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
