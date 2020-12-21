package org.openl.rules.webstudio.web.admin;

import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

public class RepositorySettingsValidators {
    public void url(FacesContext context, UIComponent toValidate, Object value) {
        validateNotBlank((String) value, "URL");
    }

    public void commentValidationPattern(FacesContext context, UIComponent toValidate, Object value) {
        String regex = (String) value;
        if (StringUtils.isBlank(regex)) {
            return;
        }

        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            WebStudioUtils.throwValidationError("Incorrect regular expression for pattern");
        }
    }

    public void ivalidCommentMessage(FacesContext context, UIComponent toValidate, Object value) {
        validateNotBlank((String) value, "Invalid user message hint");
    }

    public void commentTemplate(FacesContext context, UIComponent toValidate, Object value) {
        String template = (String) value;
        WebStudioUtils.validate(StringUtils.isNotBlank(template), "Comment message template cannot be empty");
        WebStudioUtils.validate(template.contains("{commit-type}"),
            "Comment message template must contain '{commit-type}'");
        WebStudioUtils.validate(template.contains("{user-message}"),
            "Comment message template must contain '{user-message}'");
    }

    public void newBranchNamePatternValidator(FacesContext context, UIComponent toValidate, Object value) {
        String newBranchName = StringUtils.trim((String) value);
        if (StringUtils.isNotBlank(newBranchName)) {
            newBranchName = newBranchName.replace("{project-name}", "project-name").replace("{username}", "username").replace("{current-date}", "current-date");
            WebStudioUtils.validate(!newBranchName.contains("{") && !newBranchName.contains("}"), "Only the following placeholder options are available: {project-name}, {username}, {current-date}");
            validateBranchName(newBranchName);
        }
    }

    public static void validateBranchName(String newBranchName){
        WebStudioUtils.validate(newBranchName.matches("[^\\\\:*?\"<>|{}~^\\s]*"),
                "Invalid branch name. Must not contain whitespaces or following characters: \\ : * ? \" < > | { } ~ ^");
        WebStudioUtils.validate(newBranchName.matches("(.(?<![./]{2}))+"),
                "Invalid branch name. Should not contain consecutive symbols '.' or '/'.");
        WebStudioUtils.validate(newBranchName.matches("^[^./].*[^./]"),
                "Invalid branch name. Can not start with '.' or '/'.");
    }

    protected void validateNotBlank(String value, String field) {
        WebStudioUtils.validate(StringUtils.isNotBlank(value), field + " cannot be empty");
    }
}
