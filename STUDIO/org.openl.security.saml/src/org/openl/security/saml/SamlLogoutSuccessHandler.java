package org.openl.security.saml;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml5LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2RelyingPartyInitiatedLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Finishes logout by calling logoutSuccessHandler.
 *
 * @author Eugene Biruk
 */
public class SamlLogoutSuccessHandler extends SecurityContextLogoutHandler implements LogoutHandler {

    private final Logger log = LoggerFactory.getLogger(SamlLogoutSuccessHandler.class);

    private final Saml2RelyingPartyInitiatedLogoutSuccessHandler logoutHandler;


    public SamlLogoutSuccessHandler(OpenSaml5LogoutRequestResolver requestResolver) {
        this.logoutHandler = new Saml2RelyingPartyInitiatedLogoutSuccessHandler(requestResolver);
    }

    /**
     * Causes a logout to be completed. Call logoutSuccessHandler.
     *
     * @param request        the HTTP request
     * @param response       the HTTP response
     * @param authentication the current principal details
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        super.logout(request, response, authentication);
        try {
            logoutHandler.onLogoutSuccess(request, response, authentication);
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
