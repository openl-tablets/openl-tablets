package org.openl.rules.webstudio.web.admin;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.util.StringUtils;

public class GitRepositorySettingsValidators extends RepositorySettingsValidators {
    public void localRepositoryPath(FacesContext context, UIComponent toValidate, Object value) {
        String localPath = (String) value;
        validateNotBlank(localPath, "Local path");

        String suffix = "gitLocalRepositoryPath";
        String clientId = toValidate.getClientId();
        if (clientId.endsWith(suffix)) {
            String prefix = clientId.substring(0, clientId.length() - suffix.length());
            String uri = (String) ((UIInput) context.getViewRoot().findComponent(prefix + "gitUri")).getValue();
            FacesUtils.validate(!localPath.equals(uri), "Local path and URL should not be the same");
        }
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
