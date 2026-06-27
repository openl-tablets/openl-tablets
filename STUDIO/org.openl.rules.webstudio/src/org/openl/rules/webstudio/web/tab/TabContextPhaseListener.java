package org.openl.rules.webstudio.web.tab;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;

/**
 * Establishes the request's {@link TabContext} for JSF (a4j) requests.
 *
 * <p>Runs before view restoration so the tab's identity (sent as request parameters) is available to every
 * later phase and to the beans that read the current selection. Requests that carry no tab identity (non-editing
 * pages) leave no context, so those reads fall back to the session-global selection. The context lives in
 * request scope and is released when the request ends.
 */
public class TabContextPhaseListener implements PhaseListener {

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        ExternalContext externalContext = event.getFacesContext().getExternalContext();
        TabContext context = TabContextResolver.resolve(externalContext.getRequestParameterMap()::get);
        if (context != null) {
            TabContextHolder.set(context);
        }
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        // Nothing to do; the context is request-scoped and released with the request.
    }
}
