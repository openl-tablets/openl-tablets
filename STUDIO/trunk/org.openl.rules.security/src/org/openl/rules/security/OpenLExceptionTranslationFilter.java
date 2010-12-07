package org.openl.rules.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.ExceptionTranslationFilter;

/**
 * @author Aliaksandr Antonik.
 */
public class OpenLExceptionTranslationFilter extends ExceptionTranslationFilter {
    private static final Log logger = LogFactory.getLog(OpenLExceptionTranslationFilter.class);
    private AccessDeniedHandler accessDeniedHandler = new AccessDeniedHandlerImpl();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        try {
            super.doFilter(request, response, chain);
        } catch (ServletException e) {
            logger.debug("Exception in OpenLExceptionTranslationFilter", e);
            AccessDeniedException ade = extractAccessDeniedExeption(e.getRootCause(), 50);
            if (ade != null) {
                if (getAuthenticationTrustResolver()
                        .isAnonymous(SecurityContextHolder.getContext().getAuthentication())) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Access is denied (user is anonymous); redirecting to authentication entry point",
                                e);
                    }

                    sendStartAuthentication((HttpServletRequest) request, (HttpServletResponse)response, chain,
                            new InsufficientAuthenticationException(
                                    "Full authentication is required to access this resource"));
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Access is denied (user is not anonymous); delegating to AccessDeniedHandler", e);
                    }

                    accessDeniedHandler.handle((HttpServletRequest) request, (HttpServletResponse) response, ade);
                }
            } else {
                throw e;
            }
        }
    }

    /**
     * Checks if a <code>Throwable</code> is or has somewhere down in the
     * hierarchy as a cause <code>AccessDeniedException</code>.
     *
     * @param throwable Throwable to check
     * @param depth maximum depth to recurse when checking, as we want to be
     *            nice and do not generate <code>StackOverflowError</code>
     *            when causes of the Throwable form a cycle.
     * @return first <code>AccessDeniedException</code> in cause hierarchy of
     *         <code>throwable</code> or <code>null</code> if not found
     */
    private AccessDeniedException extractAccessDeniedExeption(Throwable throwable, int depth) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof AccessDeniedException) {
            return (AccessDeniedException) throwable;
        }

        final Throwable cause = throwable.getCause();
        if (cause == null || cause == throwable || !(cause instanceof Exception) || depth == 0) {
            return null;
        }

        return extractAccessDeniedExeption(cause, depth - 1);
    }

    @Override
    public void setAccessDeniedHandler(AccessDeniedHandler accessDeniedHandler) {
        super.setAccessDeniedHandler(accessDeniedHandler);
        this.accessDeniedHandler = accessDeniedHandler;
    }
}
