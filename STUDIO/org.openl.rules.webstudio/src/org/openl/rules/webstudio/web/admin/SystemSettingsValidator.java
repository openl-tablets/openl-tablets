package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemSettingsValidator {
    private final Logger log = LoggerFactory.getLogger(SystemSettingsValidator.class);
    private final SystemSettingsBean systemSettingsBean;

    // TODO This class shouldn't depend on SystemSettingsBean
    public SystemSettingsValidator(SystemSettingsBean systemSettingsBean) {
        this.systemSettingsBean = systemSettingsBean;
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