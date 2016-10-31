package org.openl.rules.repository.jcr;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.repository.*;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.ArtefactProperties;
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
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JcrProductionRepository extends BaseJcrRepository implements RProductionRepository {
    private final Logger log = LoggerFactory.getLogger(JcrProductionRepository.class);

    public static class JCR_SQL2QueryBuilder {
        private boolean firstCondition = true;

        private void appendDateCondition(String propertyName, Date date, String condition, StringBuilder sb) {
            if (date != null) {
                if (firstCondition) {
                    firstCondition = false;
                    sb.append(" WHERE ");
                } else {
                    sb.append(" AND ");
                }

                sb.append('[').append(propertyName).append(']').append(condition).append(getDateString(date, condition));
            }
        }

        private String getDateString(Date date, String condition) {
            DateFormat format;

            if (condition.contains(">")) {
                format = new SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'");
            } else if (condition.contains("<")) {
                format = new SimpleDateFormat("yyyy-MM-dd'T'23:59:59.999'Z'");
            } else {
                format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            }

            String dateString = format.format(date);

            return "CAST('" + dateString + "' AS DATE)";
        }

        public String buildQuery(SearchParams params) {
            StringBuilder sb = new StringBuilder("SELECT * FROM [nt:base]");
            String lineOfBusiness = params.getLineOfBusiness();
            if (lineOfBusiness != null && !lineOfBusiness.isEmpty()) {
                if (firstCondition) {
                    firstCondition = false;
                    sb.append(" WHERE ");
                } else {
                    sb.append(" AND ");
                }
                // todo: check for injection
                sb.append("[" + ArtefactProperties.PROP_LINE_OF_BUSINESS + "]").append("=\"").append(lineOfBusiness).append("\"");
            }

            appendDateCondition(ArtefactProperties.PROP_EFFECTIVE_DATE, params.getLowerEffectiveDate(), ">=", sb);
            appendDateCondition(ArtefactProperties.PROP_EFFECTIVE_DATE, params.getUpperEffectiveDate(), "<=", sb);
            appendDateCondition(ArtefactProperties.PROP_EXPIRATION_DATE, params.getLowerExpirationDate(), ">=", sb);
            appendDateCondition(ArtefactProperties.PROP_EXPIRATION_DATE, params.getUpperExpirationDate(), "<=", sb);

            return sb.toString();
        }
    }

    final static String PROPERTY_NOTIFICATION = "deploymentReady";
    public static final String DEPLOY_ROOT = "/deploy";

    private Node deployLocation;
    private List<RDeploymentListener> listeners = new CopyOnWriteArrayList<RDeploymentListener>();

    public JcrProductionRepository(Session session) throws RepositoryException {
        super(session);

        deployLocation = checkPath(DEPLOY_ROOT);
        if (deployLocation.isNew()) {
            session.save();
        }

        session.getWorkspace().getObservationManager().addEventListener(this, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, DEPLOY_ROOT, false,
                null, null, false);
    }

    public void addListener(RDeploymentListener listener) {
        listeners.add(listener);
    }

    @Deprecated
    public RDeploymentDescriptorProject createDDProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public RProductionDeployment createDeployment(String name) throws RRepositoryException {
        try {
            return JcrProductionDeployment.createDeployment(deployLocation, name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("could not create deployment {0}", e, name);
        }
    }

    /**
     * Creates a project in the repository. Name of new project must be unique.
     *
     * @param name name of new project
     * @return newly created project
     * @throws org.openl.rules.repository.exceptions.RRepositoryException if
     *                                                                    failed
     */
    @Deprecated
    public RProject createProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public Collection<ArtefactAPI> findNodes(SearchParams params) throws RRepositoryException {
        try {
            Query query = getSession().getWorkspace().getQueryManager().createQuery(new JCR_SQL2QueryBuilder().buildQuery(params), Query.JCR_SQL2);
            QueryResult queryResult = query.execute();

            NodeIterator nodeIterator = queryResult.getNodes();
            List<ArtefactAPI> result = new ArrayList<ArtefactAPI>();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                ArtefactPath path = new ArtefactPathImpl(new String[]{node.getName()});
                String type = node.getPrimaryNodeType().getName();
                if (type.equals(JcrNT.NT_APROJECT)) {
                    result.add(new JcrFolderAPI(node, path));
                } else if (type.equals(JcrNT.NT_FOLDER)) {
                    result.add(new JcrFolderAPI(node, path));
                } else if (type.equals(JcrNT.NT_FILE)) {
                    result.add(new JcrFileAPI(node, path, false));
                }
            }

            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to run query", e);
        }
    }

    @Deprecated
    public RDeploymentDescriptorProject getDDProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public List<RDeploymentDescriptorProject> getDDProjects() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public RProductionDeployment getDeployment(String name) throws RRepositoryException {
        Node node;
        try {
            node = deployLocation.getNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to get node", e);
        }

        try {
            return new JcrProductionDeployment(node);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to wrap JCR node", e);
        }
    }

    public Collection<String> getDeploymentNames() throws RRepositoryException {
        List<String> result = new ArrayList<String>();
        try {
            NodeIterator iterator = deployLocation.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (node.getPrimaryNodeType().getName().equals(JcrNT.NT_DEPLOYMENT)) {
                    result.add(node.getName());
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to enumerate deployments", e);
        }

        return result;
    }

    /**
     * Gets project by name.
     *
     * @return project
     * @throws org.openl.rules.repository.exceptions.RRepositoryException if
     *                                                                    failed or no project with specified name
     */
    @Deprecated
    public RProject getProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets list of projects from the repository.
     *
     * @return list of projects
     * @throws org.openl.rules.repository.exceptions.RRepositoryException if
     *                                                                    failed
     */
    @Deprecated
    public List<RProject> getProjects() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets list of projects from the repository that are marked for deletion.
     *
     * @return list of projects that are marked for deletion
     */
    @Deprecated
    public List<RProject> getProjects4Deletion() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public boolean hasDeploymentProject(String name) throws RRepositoryException {
        try {
            return deployLocation.hasNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to check project {0}", e, name);
        }
    }

    public boolean hasDeployment(String name) throws RRepositoryException {
        try {
            return deployLocation.hasNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to check project {0}", e, name);
        }
    }

    /**
     * Checks whether project with given name exists in the repository.
     *
     * @return <code>true</code> if project with such name exists
     * @throws org.openl.rules.repository.exceptions.RRepositoryException
     */
    public boolean hasProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
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

    public FolderAPI createDeploymentProject(String name) throws RRepositoryException {
        try {
            String path = getDeploymentsRootPath() + "/" + name;
            Node parent = checkFolder(path.substring(0, path.lastIndexOf("/")));
            Node node = NodeUtil.createNode(parent, name.substring(name.lastIndexOf("/") + 1), JcrNT.NT_APROJECT, true);
            deployLocation.save();
            node.checkin();
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("", e);
        } catch (ProjectException e) {
            throw new RRepositoryException("", e);
        }
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

    public FolderAPI createRulesProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public FolderAPI getDeploymentProject(String name) throws RRepositoryException {
        Node node;
        try {
            node = deployLocation.getNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to get node", e);
        }

        try {
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to wrap JCR node", e);
        }
    }

    public List<FolderAPI> getDeploymentProjects() throws RRepositoryException {
        List<FolderAPI> result = new ArrayList<FolderAPI>();
        try {
            NodeIterator iterator = deployLocation.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (node.getPrimaryNodeType().getName().equals(JcrNT.NT_APROJECT)) {
                    result.add(new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{node.getName()})));
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to enumerate deployments", e);
        }
        return result;
    }

    @Override
    public String getDeploymentConfigRootPath() throws RRepositoryException {
        return null;
    }

    @Override
    public String getDeploymentsRootPath() throws RRepositoryException {
        try {
            return removeLeadingSlash(deployLocation.getPath());
        } catch (RepositoryException e) {
            throw new RRepositoryException(e.getMessage(), e);
        }
    }

    private String removeLeadingSlash(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public FolderAPI getRulesProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public List<FolderAPI> getRulesProjects() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRulesProjectsRootPath() throws RRepositoryException {
        return null;
    }

    public List<FolderAPI> getRulesProjectsForDeletion() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getDeploymentProjectNames() throws RRepositoryException {
        List<String> result = new ArrayList<String>();
        try {
            NodeIterator iterator = deployLocation.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (node.getPrimaryNodeType().getName().equals(JcrNT.NT_APROJECT)) {
                    result.add(node.getName());
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to enumerate deployments", e);
        }
        return result;
    }

    public void addRepositoryListener(RRepositoryListener listener) {
        throw new UnsupportedOperationException();
    }

    public void removeRepositoryListener(RRepositoryListener listener) {
        throw new UnsupportedOperationException();
    }

    public List<RRepositoryListener> getRepositoryListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isBaseNode(Node node) throws RepositoryException {
        return node.getPath().equals(deployLocation.getPath());
    }
}
