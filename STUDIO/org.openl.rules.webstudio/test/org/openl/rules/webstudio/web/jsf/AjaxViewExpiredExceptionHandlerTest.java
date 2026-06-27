package org.openl.rules.webstudio.web.jsf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.PartialResponseWriter;
import jakarta.faces.context.PartialViewContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the AJAX view-expiry recovery handler: it answers a4j requests whose view expired with a
 * well-formed partial-response error (so the client reloads the tab) and leaves all other failures and full-page
 * requests to the default handler.
 */
class AjaxViewExpiredExceptionHandlerTest {

    private static ExceptionQueuedEvent eventWith(Throwable throwable) {
        ExceptionQueuedEvent event = mock(ExceptionQueuedEvent.class);
        ExceptionQueuedEventContext context = mock(ExceptionQueuedEventContext.class);
        when(event.getContext()).thenReturn(context);
        when(context.getException()).thenReturn(throwable);
        return event;
    }

    private static FacesContext ajaxContext(boolean ajax, PartialResponseWriter writer) {
        FacesContext context = mock(FacesContext.class);
        PartialViewContext partial = mock(PartialViewContext.class);
        when(context.getPartialViewContext()).thenReturn(partial);
        when(partial.isAjaxRequest()).thenReturn(ajax);
        if (writer != null) {
            when(partial.getPartialResponseWriter()).thenReturn(writer);
            when(context.getExternalContext()).thenReturn(mock(ExternalContext.class));
        }
        return context;
    }

    @Test
    void nonAjaxRequestJustDelegates() throws Exception {
        ExceptionHandler wrapped = mock(ExceptionHandler.class);
        var handler = new AjaxViewExpiredExceptionHandler(wrapped);
        FacesContext context = ajaxContext(false, null);

        try (var faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(context);
            handler.handle();
        }

        verify(wrapped).handle();
        verify(context, never()).responseComplete();
    }

    @Test
    void ajaxViewExpiredWritesPartialErrorAndConsumesEvent() throws Exception {
        ExceptionHandler wrapped = mock(ExceptionHandler.class);
        var handler = new AjaxViewExpiredExceptionHandler(wrapped);
        List<ExceptionQueuedEvent> events = new ArrayList<>();
        events.add(eventWith(new ViewExpiredException("gone", "/x.xhtml")));
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(events);

        PartialResponseWriter writer = mock(PartialResponseWriter.class);
        FacesContext context = ajaxContext(true, writer);

        try (var faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(context);
            handler.handle();
        }

        assertTrue(events.isEmpty(), "view-expired event must be consumed");
        verify(writer).startError(ViewExpiredException.class.getName());
        verify(context).responseComplete();
        // After writing the recovery response the default handler must NOT run again on the same response.
        verify(wrapped, never()).handle();
    }

    @Test
    void ajaxOtherExceptionIsLeftToDefaultHandler() throws Exception {
        ExceptionHandler wrapped = mock(ExceptionHandler.class);
        var handler = new AjaxViewExpiredExceptionHandler(wrapped);
        List<ExceptionQueuedEvent> events = new ArrayList<>();
        events.add(eventWith(new IllegalStateException("boom")));
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(events);

        FacesContext context = ajaxContext(true, null);

        try (var faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(context);
            handler.handle();
        }

        assertEquals(1, events.size(), "non-view-expired event must remain");
        verify(context, never()).responseComplete();
        verify(wrapped).handle();
    }

    @Test
    void findsViewExpiredNestedInCauseChain() throws Exception {
        ExceptionHandler wrapped = mock(ExceptionHandler.class);
        var handler = new AjaxViewExpiredExceptionHandler(wrapped);
        List<ExceptionQueuedEvent> events = new ArrayList<>();
        events.add(eventWith(new RuntimeException("wrap", new ViewExpiredException("gone", "/x.xhtml"))));
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(events);

        PartialResponseWriter writer = mock(PartialResponseWriter.class);
        FacesContext context = ajaxContext(true, writer);

        try (var faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(context);
            handler.handle();
        }

        assertTrue(events.isEmpty());
        verify(context).responseComplete();
    }

    @Test
    void committedResponseFallsBackToDefaultHandler() throws Exception {
        ExceptionHandler wrapped = mock(ExceptionHandler.class);
        var handler = new AjaxViewExpiredExceptionHandler(wrapped);
        List<ExceptionQueuedEvent> events = new ArrayList<>();
        events.add(eventWith(new ViewExpiredException("gone", "/x.xhtml")));
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(events);

        FacesContext context = mock(FacesContext.class);
        PartialViewContext partial = mock(PartialViewContext.class);
        ExternalContext external = mock(ExternalContext.class);
        when(context.getPartialViewContext()).thenReturn(partial);
        when(partial.isAjaxRequest()).thenReturn(true);
        when(context.getExternalContext()).thenReturn(external);
        when(external.isResponseCommitted()).thenReturn(true);

        try (var faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(context);
            handler.handle();
        }

        // The response is already committed: cannot write a recovery, so defer to the default handler.
        verify(context, never()).responseComplete();
        verify(wrapped).handle();
    }
}
