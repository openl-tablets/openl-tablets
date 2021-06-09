package org.openl.rules.webstudio.web.admin;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GitRepositorySettingsValidators extends RepositorySettingsValidators {
    public void localRepositoryPath(FacesContext context, UIComponent toValidate, Object value) {
        String localPath = (String) value;
        validateNotBlank(localPath, "Local path");

        String suffix = "gitLocalRepositoryPath";
        String clientId = toValidate.getClientId();
        if (clientId.endsWith(suffix)) {
            String prefix = clientId.substring(0, clientId.length() - suffix.length());
            String uri = context.getExternalContext().getRequestParameterMap().get(prefix + "gitUri");
            WebStudioUtils.validate(!localPath.equals(uri), "Local path and URL should not be the same");
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
        WebStudioUtils.validate(email.contains("@"), "Incorrect email");
    }

    public void newBranchRegex(FacesContext context, UIComponent toValidate, Object value) {
        String pattern = (String) value;
        if (StringUtils.isBlank(pattern)) {
            return;
        }
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException patternSyntaxException) {
            WebStudioUtils.throwValidationError(
                String.format("Branch name pattern '%s' is not valid regular expression.", value)
            );
        }
    }

    @Override
    public void url(FacesContext context, UIComponent toValidate, Object value) {
        String suffix = "gitUri";
        String clientId = toValidate.getClientId();
        if (clientId.endsWith(suffix)) {
            String prefix = clientId.substring(0, clientId.length() - suffix.length());
            String remoteRepository = context.getExternalContext()
                .getRequestParameterMap()
                .get(prefix + "gitRemoteRepository");
            if ("on".equals(remoteRepository)) {
                super.url(context, toValidate, value);
            }
        }
    }
}
