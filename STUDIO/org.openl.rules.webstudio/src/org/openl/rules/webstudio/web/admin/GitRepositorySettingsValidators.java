package org.openl.rules.webstudio.web.admin;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

public class GitRepositorySettingsValidators extends RepositorySettingsValidators {
    public void localRepositoryPath(FacesContext context, UIComponent toValidate, Object value) {
        validateNotBlank((String) value, "Local path");
    }

    public void branch(FacesContext context, UIComponent toValidate, Object value) {
        validateNotBlank((String) value, "Branch");
    }
}
