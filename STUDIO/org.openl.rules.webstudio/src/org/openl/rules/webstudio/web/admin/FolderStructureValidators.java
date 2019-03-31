package org.openl.rules.webstudio.web.admin;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.util.StringUtils;

public class FolderStructureValidators {
    public void pathInRepository(FacesContext context, UIComponent toValidate, Object value) {
        String path = (String) value;

        FacesUtils.validate(StringUtils.isEmpty(path) || !path.startsWith("/"),
            "Path in repository can't start with '/'");
    }

    public void folderConfigFile(FacesContext context, UIComponent toValidate, Object value) {
        FacesUtils.validate(StringUtils.isNotBlank((String) value), "Folder config file can not be empty");
    }
}
