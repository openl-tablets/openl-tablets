package org.openl.commons.web.jsf;

import java.io.IOException;
import java.util.Map;
import java.util.Collection;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

/**
 * Various generic helpful methods to simplify common operations with JSF.
 *
 * @author Andrey Naumenko
 * @author Andrei Astrouski
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
     * Returns request parameter from HttpServletRequest object through current
     * FacesContext.
     *
     * @param parameterName parameter name
     *
     * @return parameter value - if parameter exists, <code>null</code> -
     *         otherwise.
     */
    public static String getRequestParameter(String parameterName) {
        String param = (String) getRequestParameterMap().get(parameterName);

        if (StringUtils.isNotBlank(param)) {
            try {
                param = new String(param.getBytes("ISO-8859-1"), "UTF-8");
            } catch (Exception e) {}
        }
        return param;
    }

    public static Map<String, String> getRequestParameterMap() {
        return getExternalContext().getRequestParameterMap();
    }

    public static Map<String, Object> getSessionMap() {
        return getExternalContext().getSessionMap();
    }

    public static Object getSessionParam(String name) {
        return getSessionMap().get(name);
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

    public static void redirect(String page) throws IOException {
        HttpServletResponse response = (HttpServletResponse) getResponse();
        response.sendRedirect(page);
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

}
