package org.openl.rules.webstudio.web.admin;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.util.StringUtils;

public class RepositorySettingsValidators {
    public void url(FacesContext context, UIComponent toValidate, Object value) {
        validateNotBlank((String) value, "URL");
    }

    protected void validateNotBlank(String value, String field) throws ValidatorException {
        FacesUtils.validate(StringUtils.isNotBlank(value), field + " can not be empty");
    }
}
