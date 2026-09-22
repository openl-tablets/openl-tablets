package org.openl.rules.webstudio.web.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;

import org.openl.rules.webstudio.web.servlet.RulesUserSession;

class WebStudioUtilsTest {

    @Test
    void noSessionMeansNoUserSessionAndNoWorkspace() {
        assertNull(WebStudioUtils.getRulesUserSession(null, true));
        assertNull(WebStudioUtils.getWebStudio(null));
        assertNull(WebStudioUtils.getUserWorkspace(null));
    }

    @Test
    void answersTheUserSessionTheHttpSessionHolds() {
        var rulesUserSession = new RulesUserSession();
        var session = mock(HttpSession.class);
        when(session.getAttribute(Constants.RULES_USER_SESSION)).thenReturn(rulesUserSession);

        assertSame(rulesUserSession, WebStudioUtils.getRulesUserSession(session, true));
    }
}
