package org.openl.rules.webstudio.web.admin;

import java.io.Closeable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

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
        /*Only local repo can be created*/
        this.setType(PRODUCTION_PEPOSITORY_TYPE);

        RepositoryConfiguration repoConfig = createRepositoryConfiguration();

        if (!isInputParamValid(repoConfig)) {
            return;
        }

        try {
            if (this.isSecure()) {
                RepositoryConfiguration adminConfig = this.createAdminRepositoryConfiguration();

                Repository repository = this.getProductionRepositoryFactoryProxy().getFactory(adminConfig.getProperties());

                try {
                    if (repository instanceof LocalJackrabbitRepositoryFactory) {
                        if (!((LocalJackrabbitRepositoryFactory) repository).configureJCRForOneUser(this.getLogin(), this.getPassword())) {
                            setErrorMessage("Repository user creation error");
                            return;
                        }
                    }
                } finally {
                    if (repository != null) {
                        if (repository instanceof Closeable) {
                            IOUtils.closeQuietly((Closeable) repository);
                        }
                    }
                }
            } else {
                RepositoryValidators.validateConnection(repoConfig, getProductionRepositoryFactoryProxy());
            }
        } catch (RRepositoryException e) {
            setErrorMessage(e);
        } catch (RepositoryValidationException e) {
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
