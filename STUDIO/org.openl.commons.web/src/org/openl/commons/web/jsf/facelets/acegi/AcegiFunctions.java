package org.openl.commons.web.jsf.facelets.acegi;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * As part of the EL Specification, you can incorporate static Java methods into your documents. Just add new function
 * here and it will be registered within Spring Security taglib
 *
 * @author Andrey Naumenko
 */
public final class AcegiFunctions {

    private AcegiFunctions() {
    }

    public static String authentication(String operation) {
        final Logger log = LoggerFactory.getLogger(AcegiFunctions.class);
        if ((operation == null)) {
            log.debug("Operation is not provided. Empty result string will be returned.");
            return "";
        }

        SecurityContext ctx = SecurityContextHolder.getContext();

        if ((ctx == null) || (ctx.getAuthentication() == null)) {
            log.debug("Failed to find security context or context has no authentication.");
            return "";
        }

        Authentication auth = ctx.getAuthentication();
        Object principal = null;
        if (auth.getPrincipal() instanceof UserDetails) {
            try {
                principal = auth.getPrincipal();
                log.debug("Principal is {}", principal);

                String property = BeanUtils.getProperty(principal, operation);

                if (property == null) {
                    log.debug("{} property {} is null", principal, operation);
                    return "";
                }

                return property;
            } catch (IllegalAccessException e) {
                log.warn("Error when trying to get property {} of {}. Empty string will be returned.",
                    operation,
                    principal);
                return "";
            } catch (InvocationTargetException e) {
                log.warn("Error when trying to get property {} of {}. Empty string will be returned.",
                    operation,
                    principal);
                return "";
            } catch (NoSuchMethodException e) {
                log.warn("Error when trying to get property {} of {}. Empty string will be returned.",
                    operation,
                    principal);
                return "";
            }
        } else if (auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }
        log.debug("Authentication has no principal. Or its principal is not an instanceof UserDetails");

        return "";
    }

    /**
     * Returns true if current user has access to a given URI (look for FilterSecurityInterceptor configuration)
     *
     * @param uri URI that will be checked
     * @return true if current user has access to a given URI
     */
    public static boolean hasAccessTo(String uri) {
        return getPrivilegeEvaluator().isAllowed(uri, SecurityContextHolder.getContext().getAuthentication());
    }

    private static WebInvocationPrivilegeEvaluator getPrivilegeEvaluator() {
        ApplicationContext ctx = WebApplicationContextUtils
            .getRequiredWebApplicationContext(FacesUtils.getServletContext());
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
