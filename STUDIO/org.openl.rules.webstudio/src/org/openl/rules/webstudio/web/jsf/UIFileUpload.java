package org.openl.rules.webstudio.web.jsf;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.PostAddToViewEvent;

/**
 * Workaround: fixed issue with file upload component on EDITOR page. For some reason,
 * {@value QUEUED_FILE_UPLOAD_EVENTS_ATTR} is not initialized. By default, it is initialized
 * if {@link javax.faces.event.PreRenderComponentEvent} is fired. But in our case, it is not fired.
 */
@FacesComponent(value = "openl.UIFileUpload")
@ListenerFor(systemEventClass = PostAddToViewEvent.class)
public class UIFileUpload extends org.richfaces.component.UIFileUpload {

    private static final String QUEUED_FILE_UPLOAD_EVENTS_ATTR = "queuedFileUploadEvents";

    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        super.processEvent(event);
        if (event.getSource() == this && event instanceof PostAddToViewEvent) {
            // Restore behavior as it was in 4.6.8.ayg
            getAttributes().put(QUEUED_FILE_UPLOAD_EVENTS_ATTR, new AtomicInteger());
        }
    }
}
