package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.openl.rules.webstudio.util.WebStudioValidationUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

public class SystemSettingsValidator {
    private final SystemSettingsBean systemSettingsBean;

    // TODO This class shouldn't depend on SystemSettingsBean
    public SystemSettingsValidator(SystemSettingsBean systemSettingsBean) {
        this.systemSettingsBean = systemSettingsBean;
    }

    public void dateFormatValidator(FacesContext context, UIComponent toValidate, Object value) {
        String inputDate = (String) value;

        validateNotBlank(inputDate, "Date format");

        try {
            SimpleDateFormat format = new SimpleDateFormat(inputDate);

            LocalDate dateToTest = LocalDate.of(2020, 2, 22);
            Date date = Date.from(dateToTest.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            String dateStr = format.format(date);
            Date parsedDate = format.parse(dateStr);

            WebStudioUtils.validate(parsedDate.equals(date), "format is incomplete.");
        } catch (Exception e) {
            String message = "Incorrect date format: " + e.getMessage();
            WebStudioUtils.addErrorMessage(message);
            WebStudioUtils.throwValidationError(message);
        }
    }

    public void timeFormatValidator(FacesContext context, UIComponent toValidate, Object value) {
        String timeFormat = (String) value;

        validateNotBlank(timeFormat, "Date format");

        try {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy " + timeFormat);

            // Seconds aren't obligatory.
            LocalDateTime dateTimeToTest = LocalDateTime.of(2020, 2, 22, 15, 16);
            Date date = Date.from(dateTimeToTest.atZone(ZoneId.systemDefault()).toInstant());

            String dateStr = format.format(date);
            Date parsedDate = format.parse(dateStr);

            WebStudioUtils.validate(parsedDate.equals(date), "format is incomplete.");
        } catch (Exception e) {
            String message = "Incorrect time format: " + e.getMessage();
            WebStudioUtils.addErrorMessage(message);
            WebStudioUtils.throwValidationError(message);
        }
    }

    // TODO This class shouldn't depend on SystemSettingsBean
    public void workSpaceDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        WebStudioValidationUtils.directoryValidator(value, "Workspace Directory");
        systemSettingsBean.setUserWorkspaceHome((String) value);
    }

    // TODO This class shouldn't depend on SystemSettingsBean
    public void historyDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        WebStudioValidationUtils.directoryValidator(value, "History Directory");
        systemSettingsBean.setProjectHistoryHome((String) value);
    }

    public void historyCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String errorMessage = null;
        String count = (String) value;
        validateNotBlank(count, "The maximum count of saved changes");
        if (!Pattern.matches("[0-9]+", count)) {
            errorMessage = "The maximum count of saved changes should be positive integer";
        }

        if (errorMessage != null) {
            WebStudioUtils.addErrorMessage(errorMessage);
            throw new ValidatorException(new FacesMessage(errorMessage));
        }
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
                WebStudioUtils.addErrorMessage(message);
                throw new ValidatorException(new FacesMessage(message));
            }
        } catch (NumberFormatException e) {
            WebStudioUtils.addErrorMessage(message);
            throw new ValidatorException(new FacesMessage(message));
        }
    }

    private void validateGreaterThanZero(String count, String target) {
        String message = target + " must be positive integer";
        try {
            int v = Integer.parseInt(StringUtils.trim(count));
            if (v <= 0) {
                WebStudioUtils.addErrorMessage(message);
                throw new ValidatorException(new FacesMessage(message));
            }
        } catch (NumberFormatException e) {
            WebStudioUtils.addErrorMessage(message);
            throw new ValidatorException(new FacesMessage(message));
        }
    }

    /**
     * Check permission on folder creation
     *
     * This method deletes only created folders.
     */
    private void workingDirValidator(String folderPath) {
        File folder = new File(folderPath);
        File root = null; // will be deleted, it is the first absent folder
        while (folder != null && !folder.exists()) {
            root = folder; // keep current not created
            folder = folder.getParentFile(); // get parent
        }

        File tmp = null; // temp file to check access
        boolean hasAccess = false;
        try {
            // check access
            tmp = new File(folderPath, ".openl-tmp");
            hasAccess = tmp.mkdirs();
        } finally {
            // delete all created files
            FileUtils.deleteQuietly(tmp);
            FileUtils.deleteQuietly(root);
        }
        if (!hasAccess) {
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Cannot get access to the folder ' " + folderPath + " '    Please, contact to your system administrator.",
                null));
        }
    }

    private void validateNotBlank(String value, String folderType) {
        if (StringUtils.isBlank(value)) {
            String errorMessage = folderType + " could not be empty";
            WebStudioUtils.addErrorMessage(errorMessage);
            throw new ValidatorException(new FacesMessage(errorMessage));
        }
    }
}
