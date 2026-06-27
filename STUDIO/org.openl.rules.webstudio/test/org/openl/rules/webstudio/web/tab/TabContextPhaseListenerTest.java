package org.openl.rules.webstudio.web.tab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Function;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for the JSF tab-context phase listener: it runs on RESTORE_VIEW and binds the context resolved from
 * the request parameters.
 */
class TabContextPhaseListenerTest {

    private final TabContextPhaseListener listener = new TabContextPhaseListener();

    @AfterEach
    void resetRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void runsOnRestoreView() {
        assertEquals(PhaseId.RESTORE_VIEW, listener.getPhaseId());
    }

    @SuppressWarnings("unchecked")
    @Test
    void beforePhaseBindsResolvedContext() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        FacesContext facesContext = mock(FacesContext.class);
        ExternalContext externalContext = mock(ExternalContext.class);
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestParameterMap()).thenReturn(Map.of(TabContextResolver.PARAM_PROJECT, "A"));
        PhaseEvent event = mock(PhaseEvent.class);
        when(event.getFacesContext()).thenReturn(facesContext);

        var context = new TabContext("design", null, null, null, null);
        try (var resolver = mockStatic(TabContextResolver.class)) {
            resolver.when(() -> TabContextResolver.resolve(any(Function.class))).thenReturn(context);
            listener.beforePhase(event);
            assertSame(context, TabContextHolder.get().orElseThrow());
        }
    }
}
