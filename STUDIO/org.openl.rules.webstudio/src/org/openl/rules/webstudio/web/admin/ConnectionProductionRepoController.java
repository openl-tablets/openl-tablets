package org.openl.rules.webstudio.web.admin;

import java.util.List;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.util.StringUtils;

/**
 * Used in Install Wizard only.
 *
 * @author Pavel Tarasevich
 */

@Service
@ViewScope
public class ConnectionProductionRepoController {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionProductionRepoController.class);
    private RepositoryConfiguration repositoryConfiguration;
    private boolean checked;
    private String errorMessage = "";
    @Autowired
    private SystemSettingsBean systemSettingsBean;
    @Autowired
    private RepositoryFactoryProxy productionRepositoryFactoryProxy;
    private PropertiesHolder properties;
    private List<RepositoryConfiguration> productionRepositoryConfigurations;

    public void save() {
        RepositoryConfiguration repoConfig = getRepositoryConfiguration();

        if (isInputParamInvalid(repoConfig)) {
            return;
        }

        if (!checkConnection(repoConfig)) {
            return;
        }

        // repoConfig.save();
        addProductionRepoToMainConfig(repoConfig);
        clearForm();
    }

    private boolean checkConnection(RepositoryConfiguration repoConfig) {
        setErrorMessage("");
        return checkRemoteConnection(repoConfig);
    }

    private boolean checkRemoteConnection(RepositoryConfiguration repoConfig) {
        try {
            RepositoryValidators.validateConnection(repoConfig, getProductionRepositoryFactoryProxy());
            return true;
        } catch (RepositoryValidationException e) {
            LOG.debug("Error occurred: ", e);
            setErrorMessage(e.getMessage());
            return false;
        }
    }

    @PostConstruct
    public void afterPropertiesSet() {
        setProductionRepositoryConfigurations(systemSettingsBean.getProductionRepositoryConfigurations());
        setProperties(systemSettingsBean.getProperties());
        systemSettingsBean = null;
    }

    public void init() {
        repositoryConfiguration = createRepositoryConfiguration();
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
        this.properties = properties;
    }

    public RepositoryConfiguration getRepositoryConfiguration() {
        return repositoryConfiguration;
    }

    protected RepositoryConfiguration createRepositoryConfiguration() {
        final List<RepositoryConfiguration> configurations = getProductionRepositoryConfigurations();
        String newConfigName = RepositoryEditor.getNewConfigName(configurations, RepositoryMode.PRODUCTION);
        RepositoryConfiguration repoConfig = new RepositoryConfiguration(newConfigName, properties, RepositoryType.DB.factoryId,
                configurations, RepositoryMode.PRODUCTION);
        repoConfig.commit();
        return repoConfig;
    }

    public void clearForm() {
        repositoryConfiguration = null;
        errorMessage = "";
    }

    public boolean isInputParamInvalid(RepositoryConfiguration prodConfig) {
        try {
            RepositoryValidators.validate(prodConfig, getProductionRepositoryConfigurations());

            RepositorySettings settings = prodConfig.getSettings();
            if (settings instanceof CommonRepositorySettings) {
                CommonRepositorySettings s = (CommonRepositorySettings) settings;
                if (s.isSecure() && (StringUtils.isEmpty(s.getLogin()) || StringUtils.isEmpty(s.getPassword()))) {
                    throw new RepositoryValidationException(
                            "Invalid login or password. Try again.");
                }
            }

            return false;
        } catch (RepositoryValidationException e) {
            LOG.debug("Error occurred:", e);
            this.errorMessage = e.getMessage();
            return true;
        }
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

    public RepositoryFactoryProxy getProductionRepositoryFactoryProxy() {
        return productionRepositoryFactoryProxy;
    }

    public void setProductionRepositoryFactoryProxy(RepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
