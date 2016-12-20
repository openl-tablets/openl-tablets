package org.openl.rules.repository.jcr;

import org.openl.rules.repository.api.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

public class JcrProductionRepository extends BaseJcrRepository {
    private final Logger log = LoggerFactory.getLogger(JcrProductionRepository.class);

    final static String PROPERTY_NOTIFICATION = "deploymentReady";
    public static final String DEPLOY_ROOT = "/deploy";

    private Listener listener;

    public JcrProductionRepository(Session session) throws RepositoryException {
        super(session);

        session.getWorkspace().getObservationManager().addEventListener(this, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, DEPLOY_ROOT, false,
                null, null, false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void onEvent(EventIterator eventIterator) {
        boolean activate = false;
        while (listener != null && eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            try {
                if (event.getPath().equals(DEPLOY_ROOT + "/" + PROPERTY_NOTIFICATION)) {
                    activate = true;
                    break;
                }
            } catch (RepositoryException e) {
                log.debug("onEvent-1", e);
            }
        }

        if (activate) {
                try {
                    listener.onChange();
                } catch (Exception e) {
                    log.error("onEvent-2", e);
                }
        }
    }

}
