package org.openl.studio.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;

import org.openl.rules.workspace.lw.LocalWorkspaceManager;

/**
 * Unit tests for {@link WorkspaceRegistryReconciler}.
 *
 * @author Yury Molchan
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceRegistryReconcilerTest {

    @Mock
    private LocalWorkspaceManager localWorkspaceManager;

    @Test
    void reconcilesTheWorkspaceRegistryOnInteractiveSignIn() {
        fireSignIn();

        // The reconciliation runs in the background, so the call is awaited.
        verify(localWorkspaceManager, timeout(5_000)).refreshMetainfoRegistry("jdoe");
    }

    @Test
    void signInDoesNotFailWhenReconciliationFails() {
        doThrow(new IllegalStateException("The disk is broken")).when(localWorkspaceManager)
                .refreshMetainfoRegistry("jdoe");

        assertDoesNotThrow(this::fireSignIn);
        verify(localWorkspaceManager, timeout(5_000)).refreshMetainfoRegistry("jdoe");
    }

    private void fireSignIn() {
        var event = new InteractiveAuthenticationSuccessEvent(
                new UsernamePasswordAuthenticationToken("jdoe", "N/A"), getClass());
        new WorkspaceRegistryReconciler(localWorkspaceManager).onApplicationEvent(event);
    }
}
