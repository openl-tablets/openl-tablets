package org.openl.commons.web.jsf.facelets.acegi;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * As part of the EL Specification, you can incorporate static Java methods into
 * your documents. Just add new function here and it will be registered within
 * Spring Security taglib
 *
 * @author Andrey Naumenko
 */
public class AcegiFunctions {

    public static String authentication(String operation) {
    	final Log log = LogFactory.getLog(AcegiFunctions.class);
        if ((operation == null)) {
            log.debug("Operation is not provided. Empty result string will be returned");
            return "";
        }

        SecurityContext ctx = SecurityContextHolder.getContext();

        if ((ctx == null) || (ctx.getAuthentication() == null)) {
            log.debug("Can't find security context or context has no authentication");
            return "";
        }

        Authentication auth = ctx.getAuthentication();
        Object principal = null;
        if ((auth.getPrincipal() != null) && auth.getPrincipal() instanceof UserDetails) {
            try {
                principal = auth.getPrincipal();
                if (log.isDebugEnabled()) {
                    log.debug("Principal is " + principal);
                }

                String property = BeanUtils.getProperty(principal, operation);

                if (property == null) {
                    log.debug(principal + " property  " + operation + " is null");
                    return "";
                }

                return property;
            } catch (IllegalAccessException e) {
                log.warn("Error when trying to get property " + operation + " of " + principal
                        + ". Empty string will be returned");
                return "";
            } catch (InvocationTargetException e) {
                log.warn("Error when trying to get property " + operation + " of " + principal
                        + ". Empty string will be returned");
                return "";
            } catch (NoSuchMethodException e) {
                log.warn("Error when trying to get property " + operation + " of " + principal
                        + ". Empty string will be returned");
                return "";
            }
        } else if (auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }
        if (log.isDebugEnabled()) {
            log.debug("Authentication has no principal. Or its principal is not an instanceof UserDetails");
        }

        return "";
    }

    /**
     * Returns true if current user has access to a given URI
     * (look for FilterSecurityInterceptor configuration)
     * 
     * @param uri URI that will be checked
     * @return true if current user has access to a given URI
     */
    public static boolean hasAccessTo(String uri) {
        return getPrivilegeEvaluator().isAllowed(uri, SecurityContextHolder.getContext().getAuthentication());
    }

    private static WebInvocationPrivilegeEvaluator getPrivilegeEvaluator() {
        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext((ServletContext) FacesUtils.getExternalContext().getContext());
        Map<String, WebInvocationPrivilegeEvaluator> wipes = ctx.getBeansOfType(WebInvocationPrivilegeEvaluator.class);

        if (wipes.size() == 0) {
            return new WebInvocationPrivilegeEvaluator() {

                @Override
                public boolean isAllowed(String arg0, Authentication arg1) {
                    return true;
                }

                @Override
                public boolean isAllowed(String arg0, String arg1, String arg2, Authentication arg3) {
                    return true;
                }
                
            };
        }

        return (WebInvocationPrivilegeEvaluator) wipes.values().toArray()[0];
    }
}
