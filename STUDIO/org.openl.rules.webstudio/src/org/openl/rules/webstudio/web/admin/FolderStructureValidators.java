package org.openl.rules.webstudio.web.admin;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.util.SystemReader;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

public class FolderStructureValidators {
    public void pathInRepository(FacesContext context, UIComponent toValidate, Object value) {
        String path = (String) value;

        validatePathInRepository(path);
    }

    public void folderConfigFile(FacesContext context, UIComponent toValidate, Object value) {
        String filePath = (String) value;
        WebStudioUtils.validate(StringUtils.isNotBlank(filePath), "Folder config file cannot be empty");
        validateGitPath(filePath);
    }

    public static void validatePathInRepository(String path) {
        if (StringUtils.isEmpty(path)) {
            return;
        }

        WebStudioUtils.validate(!path.startsWith("/"), "Path in repository cannot start with '/'");

        validateGitPath(path);
    }

    private static void validateGitPath(String path) {
        try {
            // Cross-platform path check
            NameChecker.validatePath(path);
        } catch (IOException e) {
            WebStudioUtils.throwValidationError(e.getMessage());
        }

        try {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            // Git specifics and non-cross-platform check if we missed something before
            SystemReader.getInstance().checkPath(path);
        } catch (CorruptObjectException e) {
            WebStudioUtils.throwValidationError(StringUtils.capitalize(e.getMessage()));
        }
    }
}
