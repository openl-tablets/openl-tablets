package org.openl.rules.repository.jcr;

import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.repository.RRepositoryListener;
import org.openl.rules.repository.RRepositoryListener.RRepositoryEvent;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation for JCR Repository. One JCR Repository instance per user.
 *
 * @author Aleh Bykhavets
 */
public class JcrRepository extends BaseJcrRepository {
    private final Logger log = LoggerFactory.getLogger(JcrRepository.class);

    private Node defRulesLocation;
    private Node defDeploymentConfigLocation;

    private List<RRepositoryListener> listeners = new ArrayList<RRepositoryListener>();

    public JcrRepository(Session session,
                         Node defRulesLocation,
                         Node defDeploymentConfigLocation)
            throws RepositoryException {
        super(session);
        this.defRulesLocation = defRulesLocation;
        this.defDeploymentConfigLocation = defDeploymentConfigLocation;

        session.getWorkspace()
                .getObservationManager()
                .addEventListener(this, Event.PROPERTY_CHANGED | Event.NODE_REMOVED, session.getRootNode().getPath(),
                        true, null, null, false);

    }

    public boolean hasDeploymentProject(String name) throws RRepositoryException {
        try {
            return defDeploymentConfigLocation.hasNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to check project ''{0}''", e, name);
        }
    }

    // ------ protected methods ------

    /**
     * {@inheritDoc}
     */
    public boolean hasProject(String name) throws RRepositoryException {
        try {
            return defRulesLocation.hasNode(name) && !defRulesLocation.getNode(name).isNodeType(JcrNT.NT_LOCK);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to check project ''{0}''", e, name);
        }
    }

    public FolderAPI createDeploymentProject(String name) throws RRepositoryException {
        try {
            Node node = NodeUtil.createNode(defDeploymentConfigLocation, name,
                    JcrNT.NT_APROJECT, true);
            defDeploymentConfigLocation.save();
            node.checkin();
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to create deploy configuration.", e);
        }
    }

    public FolderAPI createRulesProject(String name) throws RRepositoryException {
        try {
            Node node = NodeUtil.createNode(defRulesLocation, name,
                    JcrNT.NT_APROJECT, true);
            defRulesLocation.save();
            node.checkin();
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to create rules project.", e);
        }
    }

    public FolderAPI getDeploymentProject(String name) throws RRepositoryException {
        try {
            if (!defDeploymentConfigLocation.hasNode(name)) {
                throw new RRepositoryException("Cannot find Project ''{0}''.", null, name);
            }

            Node n = defDeploymentConfigLocation.getNode(name);
            return new JcrFolderAPI(n, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get DDProject ''{0}''.", e, name);
        }
    }

    public List<FolderAPI> getDeploymentProjects() throws RRepositoryException {
        NodeIterator ni;
        try {
            ni = defDeploymentConfigLocation.getNodes();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot get any deployment project", e);
        }

        LinkedList<FolderAPI> result = new LinkedList<FolderAPI>();
        while (ni.hasNext()) {
            Node n = ni.nextNode();
            try {
                if (!n.isNodeType(JcrNT.NT_LOCK)) {
                    result.add(new JcrFolderAPI(n, new ArtefactPathImpl(new String[]{n.getName()})));
                }
            } catch (RepositoryException e) {
                log.debug("Failed to add deployment project.");
            }
        }

        return result;
    }

    public FolderAPI getRulesProject(String name) throws RRepositoryException {
        try {
            if (!defRulesLocation.hasNode(name)) {
                throw new RRepositoryException("Cannot find project ''{0}''", null, name);
            }

            Node n = defRulesLocation.getNode(name);
            return new JcrFolderAPI(n, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get project ''{0}''", e, name);
        }
    }

    public List<FolderAPI> getRulesProjects() throws RRepositoryException {
        NodeIterator ni;
        try {
            ni = defRulesLocation.getNodes();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot get any rules project", e);
        }

        LinkedList<FolderAPI> result = new LinkedList<FolderAPI>();
        while (ni.hasNext()) {
            Node n = ni.nextNode();
            try {
                if (!n.isNodeType(JcrNT.NT_LOCK)) {
                    result.add(new JcrFolderAPI(n, new ArtefactPathImpl(new String[]{n.getName()})));
                }
            } catch (RepositoryException e) {
                log.debug("Failed to add rules project.");
            }
        }

        return result;
    }

    private static final String CHECKED_OUT_PROPERTY = "jcr:isCheckedOut";

    private String extractProjectName(String relativePath) {
        return new ArtefactPathImpl(relativePath).segment(0);
    }

    private boolean isProjectDeletedEvent(Event event, String relativePath) {
        ArtefactPathImpl path = new ArtefactPathImpl(relativePath);
        return path.segmentCount() == 1 && event.getType() == Event.NODE_REMOVED;
    }

    private boolean isProjectModifiedEvent(Event event, String relativePath) {
        return relativePath.contains(CHECKED_OUT_PROPERTY);
    }

    public void onEvent(EventIterator eventIterator) {
        while (eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            try {
                String path = event.getPath();
                if (path.startsWith(defRulesLocation.getPath() + "/")) {
                    String relativePath = path.substring(defRulesLocation.getPath().length() + 1);
                    if (isProjectDeletedEvent(event, relativePath) || isProjectModifiedEvent(event, relativePath)) {
                        for (RRepositoryListener listener : listeners) {
                            listener.onEventInRulesProjects(new RRepositoryEvent(extractProjectName(relativePath)));
                        }
                    }
                } else if (path.startsWith(defDeploymentConfigLocation.getPath() + "/")) {
                    String relativePath = path.substring(defDeploymentConfigLocation.getPath().length() + 1);
                    if (isProjectDeletedEvent(event, relativePath) || isProjectModifiedEvent(event, relativePath)) {
                        for (RRepositoryListener listener : listeners) {
                            listener.onEventInDeploymentProjects(new RRepositoryEvent(extractProjectName(relativePath)));
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void addRepositoryListener(RRepositoryListener listener) {
        listeners.add(listener);
    }

    public void removeRepositoryListener(RRepositoryListener listener) {
        listeners.remove(listener);
    }

    @Override
    protected boolean isBaseNode(Node node) throws RepositoryException {
        String path = node.getPath();
        return path.equals(defRulesLocation.getPath()) || path.equals(defDeploymentConfigLocation.getPath());
    }
}
