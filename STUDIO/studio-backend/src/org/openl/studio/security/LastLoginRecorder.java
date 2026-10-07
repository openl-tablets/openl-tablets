package org.openl.studio.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import org.openl.rules.webstudio.service.UserManagementService;

/**
 * Remembers the time of the last successful sign-in for every user.
 *
 * <p>Reacts on interactive sign-ins only, such as the login form or an SSO redirect.
 * Stateless requests, like Basic authentication or personal access tokens, do not start
 * a login session and are not recorded.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LastLoginRecorder implements ApplicationListener<InteractiveAuthenticationSuccessEvent> {

    private final UserManagementService userManagementService;

    @Override
    public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
        var username = event.getAuthentication().getName();
        try {
            userManagementService.recordLastLoginTime(username);
        } catch (Exception e) {
            // The listener is called within the sign-in processing, and the sign-in must not fail
            // because of a bookkeeping problem.
            log.error("Failed to record the last login time for user '{}'.", username, e);
        }
    }
}
