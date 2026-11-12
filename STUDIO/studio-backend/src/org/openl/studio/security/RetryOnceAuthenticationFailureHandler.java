package org.openl.studio.security;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * Starts the single sign-on again when the answer of the identity provider is refused.
 *
 * <p>A session keeps one pending sign-in. The answer to another one is refused: a sign-in started in a second
 * browser tab, or before the application was restarted. The user is signed in at the identity provider by then, so
 * a second attempt passes without asking anything.
 *
 * <p>Only one attempt is repeated. A sign-in that is refused again is answered {@code 401}, so a wrong setting
 * cannot send the browser round in circles. The reason of every refusal is logged.
 *
 * @author Yury Molchan
 */
@Slf4j
@RequiredArgsConstructor
class RetryOnceAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final String RETRIED = RetryOnceAuthenticationFailureHandler.class.getName() + ".retried";

    private final String loginUrl;
    private final AuthenticationFailureHandler refusal = new SimpleUrlAuthenticationFailureHandler();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("The single sign-on is refused: {}", exception.getMessage());
        var session = request.getSession();
        if (session.getAttribute(RETRIED) == null) {
            session.setAttribute(RETRIED, Boolean.TRUE);
            response.sendRedirect(request.getContextPath() + loginUrl);
        } else {
            session.removeAttribute(RETRIED);
            refusal.onAuthenticationFailure(request, response, exception);
        }
    }
}
