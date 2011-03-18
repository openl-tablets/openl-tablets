package org.openl.rules.repository.jcr;

import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.RProductionDeployment;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrProductionDeployment extends JcrProductionEntity implements RProductionDeployment {
    private static final Object lock = new Object();

    private Node node;

    /**
     * Creates new deployment instance.
     * <p>
     * Note that OpenL project cannot be created inside other OpenL project.
     * I.e. nesting is not allowed for OpenL projects.
     *
     * @param parentNode parent node
     * @param nodeName name of node
     * @return newly created deployment
     * @throws javax.jcr.RepositoryException if fails
     */
    static JcrProductionDeployment createDeployment(Node parentNode, String nodeName) throws RepositoryException {
        Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_DEPLOYMENT, false);

        parentNode.save();
        n.save();

        return new JcrProductionDeployment(n);
    }

    public JcrProductionDeployment(Node node) throws RepositoryException {
        super(node);
        NodeUtil.checkNodeType(node, JcrNT.NT_DEPLOYMENT);

        this.node = node;
    }

    public RProject createProject(String projectName) throws RRepositoryException {
        try {
            return JcrProductionProject.createProject(node, projectName);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to create project!", e);
        }
    }

    public RProject getProject(String name) throws RRepositoryException {
        try {
            return new JcrProductionProject(node.getNode(name));
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get project " + name, e);
        }
    }

    public Collection<RProject> getProjects() throws RRepositoryException {
        Collection<RProject> result = new ArrayList<RProject>();
        try {
            NodeIterator nodeIterator = node().getNodes();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                if (node.getPrimaryNodeType().getName().equals(JcrNT.NT_PROD_PROJECT)) {
                    result.add(getProject(node.getName()));
                }
            }

            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get projects!", e);
        }
    }

    public boolean hasProject(String name) throws RRepositoryException {
        try {
            return node.hasNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to check if project exists!", e);
        }
    }

    private void repositoryNotify() throws RepositoryException {
        synchronized (lock) {
            Node root = node.getParent();
            root.setProperty(JcrProductionRepository.PROPERTY_NOTIFICATION, (String) null);
            root.setProperty(JcrProductionRepository.PROPERTY_NOTIFICATION, "1");
            node.getParent().save();
        }
    }

    public void save() throws RRepositoryException {
        try {
            node.save();
            repositoryNotify();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to save deployment!", e);
        }
    }
}
