package org.openl.rules.webstudio.web.jsf;

import java.util.concurrent.atomic.AtomicInteger;
import javax.faces.component.FacesComponent;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;

/**
 * Workaround: fixed issue with file upload component on EDITOR page. For some reason,
 * {@value QUEUED_FILE_UPLOAD_EVENTS_ATTR} is not initialized. By default, it is initialized
 * if {@link javax.faces.event.PreRenderComponentEvent} is fired. But in our case, it is not fired.
 */
@FacesComponent(value = "openl.UIFileUpload")
public class UIFileUpload extends org.richfaces.component.UIFileUpload {

    private static final String QUEUED_FILE_UPLOAD_EVENTS_ATTR = "queuedFileUploadEvents";

    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        super.processEvent(event);
        if (event.getSource() == this && getAttributes().get(QUEUED_FILE_UPLOAD_EVENTS_ATTR) == null) {
            // if QUEUED_FILE_UPLOAD_EVENTS_ATTR is not initialized, initialize it!
            getAttributes().put(QUEUED_FILE_UPLOAD_EVENTS_ATTR, new AtomicInteger());
        }
    }
}
