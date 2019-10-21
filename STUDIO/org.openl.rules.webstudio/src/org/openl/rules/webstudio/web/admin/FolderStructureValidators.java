package org.openl.rules.webstudio.web.admin;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.util.SystemReader;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;

public class FolderStructureValidators {
    public void pathInRepository(FacesContext context, UIComponent toValidate, Object value) {
        String path = (String) value;

        if (StringUtils.isEmpty(path)) {
            return;
        }

        FacesUtils.validate(!path.startsWith("/"), "Path in repository cannot start with '/'");

        validateGitPath(path);
    }

    public void folderConfigFile(FacesContext context, UIComponent toValidate, Object value) {
        String filePath = (String) value;
        FacesUtils.validate(StringUtils.isNotBlank(filePath), "Folder config file cannot be empty");
        validateGitPath(filePath);
    }

    private void validateGitPath(String path) {
        try {
            // Cross-platform path check
            NameChecker.validatePath(path);
        } catch (IOException e) {
            FacesUtils.throwValidationError(e.getMessage());
        }

        try {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            // Git specifics and non-cross-platform check if we missed something before
            SystemReader.getInstance().checkPath(path);
        } catch (CorruptObjectException e) {
            FacesUtils.throwValidationError(StringUtils.capitalize(e.getMessage()));
        }
    }
}
