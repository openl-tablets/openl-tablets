package org.openl.studio.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;

import org.openl.rules.webstudio.service.UserManagementService;

/**
 * Unit tests for {@link LastLoginRecorder}.
 *
 * @author Yury Molchan
 */
@ExtendWith(MockitoExtension.class)
class LastLoginRecorderTest {

    @Mock
    private UserManagementService userManagementService;

    @Test
    void recordsLastLoginTimeOnInteractiveSignIn() {
        var authentication = new UsernamePasswordAuthenticationToken("jdoe", "N/A");
        var event = new InteractiveAuthenticationSuccessEvent(authentication, getClass());

        new LastLoginRecorder(userManagementService).onApplicationEvent(event);

        verify(userManagementService).recordLastLoginTime("jdoe");
    }

    @Test
    void signInDoesNotFailWhenRecordingFails() {
        doThrow(new IllegalStateException("The database is down")).when(userManagementService)
                .recordLastLoginTime("jdoe");
        var authentication = new UsernamePasswordAuthenticationToken("jdoe", "N/A");
        var event = new InteractiveAuthenticationSuccessEvent(authentication, getClass());
        var recorder = new LastLoginRecorder(userManagementService);

        assertDoesNotThrow(() -> recorder.onApplicationEvent(event));
    }
}
