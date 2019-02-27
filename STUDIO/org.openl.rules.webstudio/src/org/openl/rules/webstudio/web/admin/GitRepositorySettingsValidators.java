package org.openl.rules.webstudio.web.admin;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.util.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

public class GitRepositorySettingsValidators extends RepositorySettingsValidators {
    public void localRepositoryPath(FacesContext context, UIComponent toValidate, Object value) {
        validateNotBlank((String) value, "Local path");
    }

    public void branch(FacesContext context, UIComponent toValidate, Object value) {
        validateNotBlank((String) value, "Branch");
    }

    public void userEmail(FacesContext context, UIComponent toValidate, Object value) {
        String email = (String) value;
        if (StringUtils.isBlank(email)) {
            return;
        }

        // Only simple validation
        FacesUtils.validate(email.contains("@"), "Incorrect email");
    }
}
