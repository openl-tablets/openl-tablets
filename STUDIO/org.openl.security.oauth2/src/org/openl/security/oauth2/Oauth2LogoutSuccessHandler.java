package org.openl.security.oauth2;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Finishes logout by calling logoutSuccessHandler.
 *
 * @author Eugene Biruk
 */
public class Oauth2LogoutSuccessHandler extends SecurityContextLogoutHandler implements LogoutHandler {

    private final Logger log = LoggerFactory.getLogger(Oauth2LogoutSuccessHandler.class);

    private final OidcClientInitiatedLogoutSuccessHandler logoutHandler;

    public Oauth2LogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        logoutHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutHandler.setPostLogoutRedirectUri("{baseUrl}");
    }

    /**
     * Causes a logout to be completed. Call logoutSuccessHandler.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param authentication the current principal details
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        super.logout(request, response, authentication);
        try {
            logoutHandler.onLogoutSuccess(request, response, authentication);
        } catch (IOException | ServletException e) {
            log.warn(e.getMessage());
        }
    }
}
