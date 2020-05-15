package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.rules.webstudio.util.WebStudioValidationUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

public class SystemSettingsValidator {

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
            WebStudioUtils.throwValidationError(message);
        }
    }

    public void workSpaceDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        WebStudioValidationUtils.directoryValidator(value, "Workspace Directory");
    }

    public void historyDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        WebStudioValidationUtils.directoryValidator(value, "History Directory");
    }

    public void historyCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String errorMessage = null;
        String count = (String) value;
        if (!Pattern.matches("[0-9]*", count) || outOfRangeInteger(count)) {
            errorMessage = "The maximum count of saved changes should be positive integer";
        }

        if (errorMessage != null) {
            WebStudioUtils.throwValidationError(errorMessage);
        }
    }

    private boolean outOfRangeInteger(String value) {
        BigInteger enteredValue = new BigInteger(value);
        BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
        return enteredValue.compareTo(maxInt) > 0;
    }

    public void testRunThreadCountValidator(FacesContext context, UIComponent toValidate, Object value) {
        String count = (String) value;
        validateGreaterThanZero(count, "Number of threads");
    }

    private void validateGreaterThanZero(String count, String target) {
        String message = target + " must be positive integer";
        try {
            int v = Integer.parseInt(StringUtils.trim(count));
            if (v <= 0) {
                WebStudioUtils.throwValidationError(message);
            }
        } catch (NumberFormatException e) {
            WebStudioUtils.throwValidationError(message);
        }
    }

    private void validateNotBlank(String value, String folderType) {
        if (StringUtils.isBlank(value)) {
            String errorMessage = folderType + " could not be empty";
            WebStudioUtils.throwValidationError(errorMessage);
        }
    }
}
