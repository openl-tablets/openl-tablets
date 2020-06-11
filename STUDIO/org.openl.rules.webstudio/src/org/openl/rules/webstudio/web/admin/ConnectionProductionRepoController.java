package org.openl.rules.webstudio.web.admin;

import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.springframework.stereotype.Service;

/**
 * @author Pavel Tarasevich
 *
 */

@Service
@ViewScope
public class ConnectionProductionRepoController extends AbstractProductionRepoController {

    @Override
    public void save() {
        RepositoryConfiguration repoConfig = createRepositoryConfiguration();

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
            setErrorMessage(e.getMessage());
            return false;
        }
    }
}
