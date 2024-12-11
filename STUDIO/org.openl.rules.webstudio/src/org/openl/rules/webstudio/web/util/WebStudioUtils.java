package org.openl.rules.webstudio.web.util;

import java.util.Optional;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.servlet.SpringInitializer;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * Contains utility methods, which can be used from any class.
 *
 * @author Aliaksandr Antonik
 */
public abstract class WebStudioUtils {

    private static final String STUDIO_ATTR = "studio";
    private static final String TRACER_NAME = "tracer";

    public static RulesUserSession getRulesUserSession() {
        return getRulesUserSession(getSession());
    }

    public static RulesUserSession getRulesUserSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (RulesUserSession) session.getAttribute(Constants.RULES_USER_SESSION);
    }

    public static RulesUserSession getRulesUserSession(HttpSession session, boolean create) {
        RulesUserSession rulesUserSession = getRulesUserSession(session);
        if (rulesUserSession == null && create) {
            ApplicationContext appContext = SpringInitializer.getApplicationContext(session.getServletContext());
            rulesUserSession = appContext.getBean(RulesUserSession.class);
            session.setAttribute(Constants.RULES_USER_SESSION, rulesUserSession);
            // immediately add OpenL Studio to the session to be able to use it in RichFaces UI
            // it can be removed after removing RichFaces
            session.setAttribute(STUDIO_ATTR, rulesUserSession.getWebStudio());
        }
        return rulesUserSession;
    }

    public static WebStudio getWebStudio() {
        var rulesUserSession = getRulesUserSession();
        return rulesUserSession == null ? null : rulesUserSession.getWebStudio();
    }

    public static WebStudio getWebStudio(HttpSession session) {
        var rulesUserSession = getRulesUserSession(session);
        return rulesUserSession == null ? null : rulesUserSession.getWebStudio();
    }

    public static TraceHelper getTraceHelper() {
        return getTraceHelper(getSession());
    }

    public static TraceHelper getTraceHelper(HttpSession session) {
        TraceHelper traceHelper = (TraceHelper) session.getAttribute(TRACER_NAME);

        if (traceHelper == null) {
            traceHelper = new TraceHelper();
            session.setAttribute(TRACER_NAME, traceHelper);
        }

        return traceHelper;
    }

    public static WebStudio getWebStudio(boolean create) {
        HttpSession session = (HttpSession) getExternalContext().getSession(true);
        return Optional.ofNullable(getWebStudio(session))
                .orElseGet(() -> getRulesUserSession(session, true).getWebStudio());
    }

    public static boolean isStudioReady() {
        WebStudio webStudio = getWebStudio();
        return webStudio != null && webStudio.getModel().isReady();
    }

    public static ProjectModel getProjectModel() {
        return getWebStudio().getModel();
    }

    public static UserWorkspace getUserWorkspace(HttpSession session) {
        final Logger log = LoggerFactory.getLogger(WebStudioUtils.class);
        UserWorkspace userWorkspace = null;
        try {
            RulesUserSession rulesUserSession = getRulesUserSession(session, true);
            userWorkspace = rulesUserSession.getUserWorkspace();
        } catch (Exception e) {
            log.error("Failed to get user workspace", e);
        }
        return userWorkspace;
    }

    public static <T> T getBean(Class<T> clazz) {
        ServletContext servletContext = (ServletContext) getExternalContext().getContext();
        WebApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        return appContext.getBean(clazz);
    }

    @Deprecated
    public static Object getBackingBean(String beanName) {
        // workaround. Needs to find other architecture solution
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getApplication().evaluateExpressionGet(fc, "#{" + beanName + "}", Object.class);
    }

    public static HttpSession getSession() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            return request.getSession(false);
        }
        return null;
    }

    public static void throwValidationError(String message) {
        throw new ValidatorException(new FacesMessage(message));
    }

    public static void validate(boolean condition, String message) {
        if (!condition) {
            throwValidationError(message);
        }
    }

    public static void addMessage(String clientId, String summary, String detail, FacesMessage.Severity severity) {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(severity, summary, detail));
    }

    public static void addInfoMessage(String summary) {
        addMessage(null, summary, null, FacesMessage.SEVERITY_INFO);
    }

    public static void addErrorMessage(String summary) {
        addErrorMessage(summary, null);
    }

    public static void addErrorMessage(String summary, String detail) {
        addMessage(null, summary, detail, FacesMessage.SEVERITY_ERROR);
    }

    public static void addWarnMessage(String summary) {
        addMessage(null, summary, null, FacesMessage.SEVERITY_WARN);
    }

    /**
     * Returns request parameter from HttpServletRequest object through current FacesContext.
     *
     * @param parameterName parameter name
     * @return parameter value - if parameter exists, <code>null</code> - otherwise.
     */
    public static String getRequestParameter(String parameterName) {
        return getExternalContext().getRequestParameterMap().get(parameterName);
    }

    public static ExternalContext getExternalContext() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }
}
