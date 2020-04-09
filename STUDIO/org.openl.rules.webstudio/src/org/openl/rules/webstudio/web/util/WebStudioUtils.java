package org.openl.rules.webstudio.web.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

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
            rulesUserSession = new RulesUserSession();

            rulesUserSession.setUserName(
                ((CurrentUserInfo) WebApplicationContextUtils.getWebApplicationContext(session.getServletContext())
                    .getBean("currentUserInfo")).getUserName());
            rulesUserSession.setWorkspaceManager((MultiUserWorkspaceManager) WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean("workspaceManager"));
            session.setAttribute(Constants.RULES_USER_SESSION, rulesUserSession);
        }
        return rulesUserSession;
    }

    public static WebStudio getWebStudio() {
        return (WebStudio) getExternalContext().getSessionMap().get(STUDIO_ATTR);
    }

    public static WebStudio getWebStudio(HttpSession session) {
        return session == null ? null : (WebStudio) session.getAttribute(STUDIO_ATTR);
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
        WebStudio studio = getWebStudio(session);
        if (studio == null) {
            studio = new WebStudio(session);
            session.setAttribute(STUDIO_ATTR, studio);
        }
        return studio;
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

    public static <T> T getBean(String name, Class<T> clazz) {
        ServletContext servletContext = (ServletContext) getExternalContext().getContext();
        WebApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        return appContext.getBean(name, clazz);
    }

    @Deprecated
    public static Object getBackingBean(String beanName) {
        // workaround. Needs to find other architecture solution
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getApplication().evaluateExpressionGet(fc, "#{" + beanName + "}", Object.class);
    }

    public static HttpSession getSession() {
        return (HttpSession) getExternalContext().getSession(false);
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
     *
     * @return parameter value - if parameter exists, <code>null</code> - otherwise.
     */
    public static String getRequestParameter(String parameterName) {
        return getExternalContext().getRequestParameterMap().get(parameterName);
    }

    public static ExternalContext getExternalContext() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }
}
