package org.openl.rules.webstudio.web.jsf.util;

import java.util.Map;

import javax.faces.context.FacesContext;


/**
 * Various generic helpful methods to simplify common operations with JSF.
 *
 * @author Andrey Naumenko
 */
public abstract class FacesUtils {
    /**
     * Returns variable from faces context by name using ValueBinding.
     *
     * @param name bean name.
     *
     * @return bean from context.
     */
    public static Object getFacesVariable(String name) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext.getApplication().createValueBinding(getBinding(name))
            .getValue(facesContext);
    }

    private static String getBinding(String name) {
        if (name.startsWith("#{")) {
            return name;
        }
        return "#{" + name + "}";
    }

    @SuppressWarnings("unchecked")
    public static Map getSessionMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }

    @SuppressWarnings("unchecked")
    public static Map getRequestParameterMap() {
        return FacesContext.getCurrentInstance().getExternalContext()
            .getRequestParameterMap();
    }

    /**
     * Returns request parameter from HttpServletRequest object through current
     * FacesContext.
     *
     * @param parameterName parameter name
     *
     * @return parameter value - if parameter exists, <code>null</code> - otherwise.
     */
    public static String getRequestParameter(String parameterName) {
        return (String) getRequestParameterMap().get(parameterName);
    }
}
