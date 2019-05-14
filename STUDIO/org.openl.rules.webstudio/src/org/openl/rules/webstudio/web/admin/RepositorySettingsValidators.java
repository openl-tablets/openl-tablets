package org.openl.rules.webstudio.web.admin;

import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.commons.web.jsf.FacesUtils;
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
            FacesUtils.throwValidationError("Incorrect regular expression for pattern");
        }
    }

    public void ivalidCommentMessage(FacesContext context, UIComponent toValidate, Object value) {
        validateNotBlank((String) value, "Invalid user message hint");
    }

    public void commentTemplate(FacesContext context, UIComponent toValidate, Object value) {
        String template = (String) value;
        FacesUtils.validate(StringUtils.isNotBlank(template), "Comment message template can't be empty");
        FacesUtils.validate(template.contains("{commit-type}"), "Comment message template must contain '{commit-type}'");
        FacesUtils.validate(template.contains("{user-message}"), "Comment message template must contain '{user-message}'");
    }

    protected void validateNotBlank(String value, String field) {
        FacesUtils.validate(StringUtils.isNotBlank(value), field + " can not be empty");
    }
}
