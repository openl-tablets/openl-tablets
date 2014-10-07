package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.net.ConnectException;
import java.util.List;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.jcr.LoginException;
import javax.security.auth.login.FailedLoginException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemSettingsValidator {
    private static final Pattern PROHIBITED_CHARACTERS = Pattern.compile("[\\p{Punct}]+");

    private final Logger log = LoggerFactory.getLogger(SystemSettingsValidator.class);
    private final SystemSettingsBean systemSettingsBean;

    // TODO This class shouldn't depend on SystemSettingsBean
    public SystemSettingsValidator(SystemSettingsBean systemSettingsBean) {
        this.systemSettingsBean = systemSettingsBean;
    }

    public void validate(RepositoryConfiguration prodConfig,
            List<RepositoryConfiguration> productionRepositoryConfigurations) throws RepositoryValidationException {
        if (StringUtils.isEmpty(prodConfig.getName())) {
            String msg = "Repository name is empty. Please, enter repository name";
            throw new RepositoryValidationException(msg);
        }
        if (StringUtils.isEmpty(prodConfig.getPath())) {
            String msg = "Repository path is empty. Please, enter repository path";
            throw new RepositoryValidationException(msg);
        }

        if (PROHIBITED_CHARACTERS.matcher(prodConfig.getName()).find()) {
            String msg = String.format(
                    "Repository name '%s' contains illegal characters. Please, correct repository name",
                    prodConfig.getName());
            throw new RepositoryValidationException(msg);
        }

        // workingDirValidator(prodConfig.getPath(),
        // "Production Repository directory");

        // Check for name uniqueness.
        for (RepositoryConfiguration other : productionRepositoryConfigurations) {
            if (other != prodConfig) {
                if (prodConfig.getName().equals(other.getName())) {
                    String msg = String.format("Repository name '%s' already exists. Please, insert a new one",
                            prodConfig.getName());
                    throw new RepositoryValidationException(msg);
                }

                if (prodConfig.getPath().equals(other.getPath())) {
                    String msg = String.format("Repository path '%s' already exists. Please, insert a new one",
                            prodConfig.getPath());
                    throw new RepositoryValidationException(msg);
                }
            }
        }
    }

    public void validateConnection(RepositoryConfiguration repoConfig,
            ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) throws RepositoryValidationException {
        try {
            /**Close connection to jcr before checking connection*/
            productionRepositoryFactoryProxy.releaseRepository(repoConfig.getConfigName());
            RRepositoryFactory repoFactory = productionRepositoryFactoryProxy.getFactory(
                    repoConfig.getProperties());

            RRepository repository = repoFactory.getRepositoryInstance();
            /*Close repo connection after validation*/
            repository.release();
            productionRepositoryFactoryProxy.releaseRepository(repoConfig.getConfigName());
        } catch (RRepositoryException e) {
            Throwable resultException = ExceptionUtils.getRootCause(e);
            if (resultException == null) {
                resultException = e;
            }

            if (resultException instanceof LoginException) {
                if (!repoConfig.isSecure()) {
                    throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                            + "\" : Connection is secure. Please, insert login and password");
                } else {
                    throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                            + "\" : Invalid login or password. Please, check login and password");
                }
            } else if (resultException instanceof FailedLoginException) {
                throw new RepositoryValidationException("Repository \"" + repoConfig.getName()
                        + "\" : Invalid login or password. Please, check login and password");
            } else if (resultException instanceof ConnectException) {
                throw new RepositoryValidationException("Connection refused. Please, check repository URL");
            }

            throw new RepositoryValidationException("Repository \"" + repoConfig.getName() + "\" : "
                    + resultException.getMessage());
        }
    }

    public void dateFormatValidator(FacesContext context, UIComponent toValidate, Object value) {
        String inputDate = (String) value;

        validateNotBlank(inputDate, "Date format");

    }

    // TODO This class shouldn't depend on SystemSettingsBean
    public void workSpaceDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        String directoryType = "Workspace Directory";
        validateNotBlank((String) value, directoryType);
        systemSettingsBean.setUserWorkspaceHome((String) value);
        workingDirValidator(systemSettingsBean.getUserWorkspaceHome(), directoryType);

    }

    // TODO This class shouldn't depend on SystemSettingsBean
    public void historyDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        String directoryType = "History Directory";
        validateNotBlank((String) value, directoryType);
        systemSettingsBean.setProjectHistoryHome((String) value);
        workingDirValidator(systemSettingsBean.getProjectHistoryHome(), directoryType);

    }

    public void historyCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String errorMessage = null;
        String count = (String) value;
        validateNotBlank(count, "The maximum count of saved changes");
        if (!Pattern.matches("[0-9]+", count)) {
            errorMessage = "The maximum count of saved changes should be positive integer";
        }

        if (errorMessage != null) {
            FacesUtils.addErrorMessage(errorMessage);
            throw new ValidatorException(new FacesMessage(errorMessage));
        }
    }

    public void maxCachedProjectsCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String count = (String) value;
        validateNotNegativeInteger(count, "The maximum number of cached projects");
    }

    public void cachedProjectIdleTimeValidator(FacesContext context, UIComponent toValidate, Object value) {
        String count = (String) value;
        validateNotNegativeInteger(count, "The time to store a project in cache");
    }

    public void testRunThreadCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String count = (String) value;
        validateGreaterThanZero(count, "Number of threads");
    }

    private void validateNotNegativeInteger(String count, String target) {
        String message = target + " must be positive integer or zero";
        try {
            int v = Integer.parseInt(StringUtils.trim(count));
            if (v < 0) {
                FacesUtils.addErrorMessage(message);
                throw new ValidatorException(new FacesMessage(message));
            }
        } catch (NumberFormatException e) {
            FacesUtils.addErrorMessage(message);
            throw new ValidatorException(new FacesMessage(message));
        }
    }

    private void validateGreaterThanZero(String count, String target) {
        String message = target + " must be positive integer";
        try {
            int v = Integer.parseInt(StringUtils.trim(count));
            if (v <= 0) {
                FacesUtils.addErrorMessage(message);
                throw new ValidatorException(new FacesMessage(message));
            }
        } catch (NumberFormatException e) {
            FacesUtils.addErrorMessage(message);
            throw new ValidatorException(new FacesMessage(message));
        }
    }

    public void workingDirValidator(String value, String folderType) {
        File studioWorkingDir;
        File tmpFile = null;
        boolean hasAccess;

        try {

            studioWorkingDir = new File(value);

            if (studioWorkingDir.exists()) {
                tmpFile = new File(studioWorkingDir.getAbsolutePath() + File.separator + "tmp");

                hasAccess = tmpFile.mkdir();

                if (!hasAccess) {
                    throw new ValidatorException(new FacesMessage("Can't get access to the folder ' " + value
                            + " '    Please, contact to your system administrator."));
                }
            } else {
                if (!studioWorkingDir.mkdirs()) {
                    throw new ValidatorException(new FacesMessage("Incorrect " + folderType + " '" + value + "'"));
                } else {
                    deleteFolder(value);
                }
            }
        } catch (Exception e) {
            FacesUtils.addErrorMessage(e.getMessage());
            throw new ValidatorException(new FacesMessage(e.getMessage()));
        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    private void validateNotBlank(String value, String folderType) throws ValidatorException {
        if (StringUtils.isBlank(value)) {
            String errorMessage = folderType + " could not be empty";
            FacesUtils.addErrorMessage(errorMessage);
            throw new ValidatorException(new FacesMessage(errorMessage));
        }
    }

    /**
     * Deleting the folder which was created for folder permissions validation
     *
     * @deprecated This method deletes all parent folders if they are not locked - this is error-prone especially in non-Windows
     * OS. Remove it and delete only created folders.
     */
    @Deprecated
    private void deleteFolder(String folderPath) {
        File workFolder = new File(folderPath);
        File parent = workFolder.getParentFile();

        while (parent != null) {
            if (!workFolder.delete()) {
                log.warn("The folder '{}' isn't deleted", workFolder.getName());
            }
            parent = workFolder.getParentFile();
            workFolder = parent;
        }
    }
}