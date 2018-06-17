package org.openl.commons.web.jsf;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Various generic helpful methods to simplify common operations with JSF.
 *
 * @author Andrey Naumenko
 * @author Andrei Astrouski
 */
public abstract class FacesUtils {
    /**
     * Creates an array of <code>SelectItem</code>s from collection of <code>String</code>s.
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
     * Creates an array of <code>SelectItem</code>s from array of <code>String</code>s.
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

    /**
     * Creates an array of <code>SelectItem</code>.
     *
     * @param values an array of values.
     * @param labels an array of labels.
     * @return array of JSF objects representing items.
     */
    public static SelectItem[] createSelectItems(String[] values, String[] labels) {
        SelectItem[] items = new SelectItem[values.length];
        for (int i = 0; i < items.length; ++i) {
            items[i] = new SelectItem(values[i], labels[i]);
        }
        return items;
    }

    public static FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    public static ValueExpression createValueExpression(String expressionString) {
        FacesContext context = FacesContext.getCurrentInstance();
        ELContext elContext = context.getELContext();
        ValueExpression valueExpression = context.getApplication().getExpressionFactory().
            createValueExpression(elContext, expressionString, Object.class);
        return valueExpression;
    }

    public static MethodExpression createMethodExpression(String expressionString) {
        return createMethodExpression(expressionString, null);
    }

    public static MethodExpression createMethodExpression(String expressionString, Class<?>[] paramTypes) {
        FacesContext context = FacesContext.getCurrentInstance();
        ELContext elContext = context.getELContext();
        MethodExpression methodExpression = context.getApplication().getExpressionFactory().
            createMethodExpression(elContext, expressionString, null,
                    paramTypes == null ? new Class[0] : paramTypes);
        return methodExpression;
    }

    public static Object getValueExpressionValue(String expressionString) {
        ValueExpression valueExpression = createValueExpression(expressionString);
        return valueExpression.getValue(getELContext());
    }

    public static Object getBackingBean(String beanName) {
        return getValueExpressionValue("#{" + beanName + "}");
    }

    public static Object getValueExpressionValue(UIComponent component, String componentParam) {
        ValueExpression valueExpression = component.getValueExpression(componentParam);
        if (valueExpression != null) {
            return valueExpression.getValue(getELContext());
        }
        return null;
    }

    public static String getValueExpressionString(UIComponent component, String componentParam) {
        ValueExpression valueExpression = component.getValueExpression(componentParam);
        if (valueExpression != null) {
            return valueExpression.getExpressionString();
        }
        return null;
    }

    public static Object invokeMethodExpression(String expressionString) {
        return invokeMethodExpression(expressionString, null, null);
    }

    public static Object invokeMethodExpression(String expressionString, Object[] params, Class<?>[] paramTypes) {
        MethodExpression methodExpression = createMethodExpression(expressionString, paramTypes);
        return methodExpression.invoke(getELContext(), params == null ? new Object[0] : params);
    }

    public static ELContext getELContext() {
        ELContext elContext = getFacesContext().getELContext();
        return elContext;
    }

    public static Map<String, Object> getRequestMap() {
        return getExternalContext().getRequestMap();
    }

    /**
     * Returns request parameter from HttpServletRequest object through current FacesContext.
     *
     * @param parameterName parameter name
     *
     * @return parameter value - if parameter exists, <code>null</code> - otherwise.
     */
    public static String getRequestParameter(String parameterName) {
        return getRequestParameterMap().get(parameterName);
    }

    public static int getRequestIntParameter(String name, int defaultValue) {
        String value = getRequestParameter(name);
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Map<String, String> getRequestParameterMap() {
        return getExternalContext().getRequestParameterMap();
    }

    public static ServletContext getServletContext() {
        return (ServletContext) getExternalContext().getContext();
    }

    public static Map<String, Object> getSessionMap() {
        return getExternalContext().getSessionMap();
    }

    public static Object getSessionParam(String name) {
        return getSessionMap().get(name);
    }

    public static void removeSessionParam(String name) {
        getSessionMap().remove(name);
    }

    public static ExternalContext getExternalContext() {
        return getFacesContext().getExternalContext();
    }

    public static ServletRequest getRequest() {
        return (ServletRequest) getExternalContext().getRequest();
    }

    public static ServletResponse getResponse() {
        return (ServletResponse) getExternalContext().getResponse();
    }

    public static HttpSession getSession(boolean create) {
        return (HttpSession) getExternalContext().getSession(create);
    }

    public static HttpSession getSession() {
        return getSession(false);
    }

    public static String getContextPath() {
        return getExternalContext().getRequestContextPath();
    }

    public static Cookie[] getCookies() {
        return ((HttpServletRequest) getRequest()).getCookies();
    }

    public static void addCookie(Cookie cookie) {
        ((HttpServletResponse) getResponse()).addCookie(cookie);
    }

    public static void addCookie(String name, String value, int age) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(((HttpServletRequest) getRequest()).getContextPath());
        cookie.setMaxAge(age);
        FacesUtils.addCookie(cookie);
    }

    public static void redirect(String page) throws IOException {
        HttpServletResponse response = (HttpServletResponse) getResponse();
        response.sendRedirect(page);
    }

    public static void redirectToRoot() throws IOException {
        redirect(FacesUtils.getContextPath() + "/");
    }

    public static void addMessage(String summary, Severity severity) {
        addMessage(summary, null, severity);
    }

    public static void addMessage(String summary, String detail, Severity severity) {
        addMessage(null, summary, detail, severity);
    }

    public static void addMessage(String clientId, String summary, String detail, Severity severity) {
        getFacesContext().addMessage(clientId, new FacesMessage(severity, summary, detail));
    }

    public static void addInfoMessage(String summary) {
        addInfoMessage(summary, null);
    }

    public static void addInfoMessage(String summary, String detail) {
        addMessage(summary, detail, FacesMessage.SEVERITY_INFO);
    }

    public static void addErrorMessage(String summary) {
        addErrorMessage(summary, null);
    }

    public static void addErrorMessage(String summary, String detail) {
        addMessage(summary, detail, FacesMessage.SEVERITY_ERROR);
    }

    public static void addWarnMessage(String summary) {
        addWarnMessage(summary, null);
    }

    public static void addWarnMessage(String summary, String detail) {
        addMessage(summary, detail, FacesMessage.SEVERITY_WARN);
    }

    public static FacesMessage createErrorMessage(String summary) {
        return createErrorMessage(summary, null);
    }

    public static FacesMessage createErrorMessage(String summary, String detail) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail);
    }

    public static FacesMessage createWarnMessage(String summary) {
        return createWarnMessage(summary, null);
    }

    public static FacesMessage createWarnMessage(String summary, String detail) {
        return new FacesMessage(FacesMessage.SEVERITY_WARN, summary, detail);
    }

    public static FacesMessage createInfoMessage(String summary) {
        return createInfoMessage(summary, null);
    }

    public static FacesMessage createInfoMessage(String summary, String detail) {
        return new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail);
    }

    public static void throwValidationError(String message) {
        throw new ValidatorException(new FacesMessage(message));
    }

    public static void validate(boolean condition, String message) {
        if (!condition) {
            throwValidationError(message);
        }
    }

    public static void validateAndAddErrorMessage(boolean condition, String message) {
        if (!condition) {
            addErrorMessage(message);
            throwValidationError(message);
        }
    }
}
