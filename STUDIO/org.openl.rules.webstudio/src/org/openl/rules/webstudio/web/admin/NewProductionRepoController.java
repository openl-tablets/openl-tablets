package org.openl.rules.webstudio.web.admin;

import java.io.Closeable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.factories.LocalJackrabbitRepositoryFactory;
import org.openl.util.IOUtils;

/**
 * @author Pavel Tarasevich
 *
 */

@ManagedBean
@ViewScoped

public class NewProductionRepoController extends AbstractProductionRepoController {
    private static final String PRODUCTION_PEPOSITORY_TYPE = "local";

    @Override
    public void save() {
        /* Only local repo can be created */
        getRepositoryConfiguration().setType(PRODUCTION_PEPOSITORY_TYPE);

        RepositoryConfiguration repoConfig = createRepositoryConfiguration();

        if (isInputParamInvalid(repoConfig)) {
            return;
        }

        try {
            RepositorySettings settings = getRepositoryConfiguration().getSettings();

            if (settings instanceof CommonRepositorySettings && ((CommonRepositorySettings) settings).isSecure()) {
                CommonRepositorySettings s = (CommonRepositorySettings) settings;
                RepositoryConfiguration adminConfig = this.createAdminRepositoryConfiguration();

                Repository repository = RepositoryFactoryInstatiator.newFactory(adminConfig.getProperties(),
                    RepositoryMode.PRODUCTION);

                try {
                    if (repository instanceof LocalJackrabbitRepositoryFactory && !((LocalJackrabbitRepositoryFactory) repository)
                        .configureJCRForOneUser(s.getLogin(), s.getPassword())) {
                        setErrorMessage("Repository user creation error");
                        return;
                    }
                } finally {
                    if (repository instanceof Closeable) {
                        IOUtils.closeQuietly((Closeable) repository);
                    }
                }
            } else {
                RepositoryValidators.validateConnection(repoConfig, getProductionRepositoryFactoryProxy());
            }
        } catch (RRepositoryException | RepositoryValidationException e) {
            setErrorMessage(e);
        }

        addProductionRepoToMainConfig(repoConfig);

        clearForm();
    }

    private void setErrorMessage(Throwable exception) {
        Throwable resultException = exception;

        while (resultException.getCause() != null) {
            resultException = resultException.getCause();
        }

        setErrorMessage(resultException.getMessage());
    }

}
