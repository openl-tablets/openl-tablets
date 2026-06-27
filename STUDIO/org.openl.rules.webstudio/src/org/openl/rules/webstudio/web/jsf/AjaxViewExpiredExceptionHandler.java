package org.openl.rules.webstudio.web.jsf;

import java.io.IOException;
import java.util.Iterator;
import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.PartialResponseWriter;
import jakarta.faces.event.ExceptionQueuedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Recovers from a {@link ViewExpiredException} raised during an AJAX (a4j) request.
 *
 * <p>When the server-side JSF view backing a tab is gone (the session view cache was evicted or the server
 * restarted), an a4j postback fails to restore the view. Without recovery the partial response is malformed,
 * so the RichFaces status spinner never stops and the failure is logged repeatedly. This handler instead emits
 * a well-formed partial-response error naming {@code ViewExpiredException}, which the page's AJAX error handler
 * recognizes and uses to reload the tab (the browser still holds the tab's location, so the reload restores it).
 *
 * <p>Only AJAX requests are handled here. Full-page requests keep the default behavior (the {@code web.xml}
 * error page for {@link ViewExpiredException}).
 */
@Slf4j
public class AjaxViewExpiredExceptionHandler extends ExceptionHandlerWrapper {

    public AjaxViewExpiredExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    @Override
    public void handle() throws FacesException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getPartialViewContext().isAjaxRequest() && recoverFromViewExpired(context)) {
            // The AJAX response was written here; do not let the default handler write to it again.
            return;
        }
        getWrapped().handle();
    }

    /**
     * Remove every queued view-expiry from the unhandled queue and, if any was present and the response is not
     * yet committed, answer the AJAX request with a partial-response error so the client can reload the tab.
     *
     * @return {@code true} when the AJAX response was written (the caller must not write again)
     */
    private boolean recoverFromViewExpired(FacesContext context) {
        Iterator<ExceptionQueuedEvent> events = getUnhandledExceptionQueuedEvents().iterator();
        ViewExpiredException expired = null;
        while (events.hasNext()) {
            ViewExpiredException viewExpired = asViewExpired(events.next());
            if (viewExpired != null) {
                expired = viewExpired;
                events.remove();
            }
        }
        if (expired == null || context.getExternalContext().isResponseCommitted()) {
            return false;
        }
        writePartialError(context, expired);
        return true;
    }

    /**
     * The {@link ViewExpiredException} in the event's exception chain, or {@code null} when the event is some
     * other failure.
     */
    private ViewExpiredException asViewExpired(ExceptionQueuedEvent event) {
        Throwable throwable = event.getContext().getException();
        while (throwable != null) {
            if (throwable instanceof ViewExpiredException viewExpired) {
                return viewExpired;
            }
            throwable = throwable.getCause();
        }
        return null;
    }

    private void writePartialError(FacesContext context, ViewExpiredException expired) {
        ExternalContext externalContext = context.getExternalContext();
        externalContext.setResponseContentType("text/xml");
        externalContext.setResponseCharacterEncoding("UTF-8");
        externalContext.addResponseHeader("Cache-Control", "no-cache");
        String message = expired.getMessage() != null ? expired.getMessage() : "View could not be restored.";
        try {
            PartialResponseWriter writer = context.getPartialViewContext().getPartialResponseWriter();
            writer.startDocument();
            writer.startError(ViewExpiredException.class.getName());
            writer.write(message);
            writer.endError();
            writer.endDocument();
            context.responseComplete();
            log.debug("Recovered from ViewExpiredException on AJAX request: {}", message);
        } catch (IOException e) {
            throw new FacesException(e);
        }
    }
}
