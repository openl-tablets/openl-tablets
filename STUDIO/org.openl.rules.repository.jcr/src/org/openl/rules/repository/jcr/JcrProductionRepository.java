package org.openl.rules.repository.jcr;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.repository.*;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JcrProductionRepository extends BaseJcrRepository implements RProductionRepository {
    private final Logger log = LoggerFactory.getLogger(JcrProductionRepository.class);

    final static String PROPERTY_NOTIFICATION = "deploymentReady";
    public static final String DEPLOY_ROOT = "/deploy";

    private Node deployLocation;
    private List<RDeploymentListener> listeners = new CopyOnWriteArrayList<RDeploymentListener>();

    public JcrProductionRepository(Session session, Node deployLocation) throws RepositoryException {
        super(session);
        this.deployLocation = deployLocation;

        session.getWorkspace().getObservationManager().addEventListener(this, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, DEPLOY_ROOT, false,
                null, null, false);
    }

    public void addListener(RDeploymentListener listener) {
        listeners.add(listener);
    }

    public void onEvent(EventIterator eventIterator) {
        boolean activate = false;
        while (eventIterator.hasNext()) {
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
            for (RDeploymentListener listener : listeners) {
                try {
                    listener.onEvent();
                } catch (Exception e) {
                    log.error("onEvent-2", e);
                }
            }

        }
    }

    public boolean removeListener(RDeploymentListener listener) {
        return listeners.remove(listener);
    }

    //FIXME
    private static final Object lock = new Object();

    public void notifyChanges() throws RRepositoryException {
        synchronized (lock) {
            try {
                deployLocation.setProperty(JcrProductionRepository.PROPERTY_NOTIFICATION, System.currentTimeMillis());
                deployLocation.save();
            } catch (RepositoryException e) {
                throw new RRepositoryException("Failed to notify changes", e);
            }
        }
    }

    public void addRepositoryListener(RRepositoryListener listener) {
        throw new UnsupportedOperationException();
    }

    public void removeRepositoryListener(RRepositoryListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isBaseNode(Node node) throws RepositoryException {
        return node.getPath().equals(deployLocation.getPath());
    }
}
