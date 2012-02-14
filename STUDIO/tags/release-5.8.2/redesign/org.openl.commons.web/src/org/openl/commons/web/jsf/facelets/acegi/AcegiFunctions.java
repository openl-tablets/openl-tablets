package org.openl.commons.web.jsf.facelets.acegi;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.InvocationTargetException;

/**
 * As part of the EL Specification, you can incorporate static Java methods into
 * your documents. Just add new function here and it will be registered within
 * Spring Security taglib
 *
 * @author Andrey Naumenko
 */
public class AcegiFunctions {
    private static final Log log = LogFactory.getLog(AcegiFunctions.class);

    public static String authentication(String operation) {
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
}
