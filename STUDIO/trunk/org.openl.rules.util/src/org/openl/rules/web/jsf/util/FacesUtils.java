package org.openl.rules.web.jsf.util;

import java.util.Map;
import java.util.Collection;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.FactoryFinder;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.model.SelectItem;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Various generic helpful methods to simplify common operations with JSF.
 *
 * @author Andrey Naumenko
 */
public abstract class FacesUtils {
    /**
     * Creates an array of <code>SelectItem</code>s from collection of
     * <code>String</code>s;
     *
     * @param values an array of <code>SelectItem</code> values.
     * @return array of JSF objects representing items.
     */
    public static SelectItem[] createSelectItems(Collection<String> values) {
        SelectItem[] items = new SelectItem[values.size()];
        int index = 0;
        for (String value : values) {
            items[index++] = new SelectItem(value);
        }
        return items;
    }

    /**
     * Creates an array of <code>SelectItem</code>s from array of
     * <code>String</code>s;
     *
     * @param values an array of <code>SelectItem</code> values.
     * @return array of JSF objects representing items.
     */
    public static SelectItem[] createSelectItems(String[] values) {
        SelectItem[] items = new SelectItem[values.length];
        for (int i = 0; i < items.length; ++i) {
            items[i] = new SelectItem(values[i]);
        }
        return items;
    }

    private static String getBinding(String name) {
        if (name.startsWith("#{")) {
            return name;
        }
        return "#{" + name + "}";
    }

    public static FacesContext getFacesContext(HttpServletRequest request, HttpServletResponse response) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            FacesContextFactory contextFactory = (FacesContextFactory) FactoryFinder
                    .getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
            LifecycleFactory lifecycleFactory = (LifecycleFactory) FactoryFinder
                    .getFactory(FactoryFinder.LIFECYCLE_FACTORY);
            Lifecycle lifecycle = lifecycleFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);
            facesContext = contextFactory.getFacesContext(request.getSession().getServletContext(), request, response,
                    lifecycle);
        }
        return facesContext;
    }

    /**
     * Returns variable from faces context by name using ValueBinding.
     *
     * @param name bean name.
     *
     * @return bean from context.
     */
    public static Object getFacesVariable(String name) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext.getApplication().createValueBinding(getBinding(name)).getValue(facesContext);
    }

    public static MethodExpression createMethodExpression(String expressionString) {
        FacesContext context = FacesContext.getCurrentInstance();
        ELContext elContext = context.getELContext();
        MethodExpression methodExpression = context.getApplication().getExpressionFactory().
            createMethodExpression(elContext, expressionString, null, new Class[0]);
        return methodExpression;
    }

    public static Object invokeMethodExpression(String expressionString) {
        MethodExpression methodExpression = createMethodExpression(expressionString);
        return methodExpression.invoke(getELContext(), new Object[0]);
    }

    public static ELContext getELContext() {
        FacesContext context = FacesContext.getCurrentInstance();
        ELContext elContext = context.getELContext();
        return elContext;
    }

    @SuppressWarnings("unchecked")
    public static Map getRequestMap() {
        return getExternalContext().getRequestMap();
    }

    /**
     * Returns request parameter from HttpServletRequest object through current
     * FacesContext.
     *
     * @param parameterName parameter name
     *
     * @return parameter value - if parameter exists, <code>null</code> -
     *         otherwise.
     */
    public static String getRequestParameter(String parameterName) {
        return (String) getRequestParameterMap().get(parameterName);
    }

    @SuppressWarnings("unchecked")
    public static Map getRequestParameterMap() {
        return getExternalContext().getRequestParameterMap();
    }

    @SuppressWarnings("unchecked")
    public static Map getSessionMap() {
        return getExternalContext().getSessionMap();
    }

    public static ExternalContext getExternalContext() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }

    public static ServletRequest getRequest() {
        return (ServletRequest) getExternalContext().getRequest();
    }

    public static HttpSession getSession() {
        return (HttpSession) getExternalContext().getSession(false);
    }

    public static String getContextPath() {
        return getExternalContext().getRequestContextPath();
    }

}
