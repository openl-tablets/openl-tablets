package org.openl.rules.repository.jcr;

import java.util.Collection;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.RDeploymentDescriptorProject;
import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrDeploymentDescriptorProject extends JcrCommonProject implements RDeploymentDescriptorProject {
    private HashMap<String, RProjectDescriptor> projects;

    protected static JcrDeploymentDescriptorProject createProject(Node parentNode, String name)
            throws RepositoryException {
        Node n = NodeUtil.createNode(parentNode, name, JcrNT.NT_DEPLOYMENT_PROJECT, true);

        parentNode.save();
        n.checkin();
        n.save();

        return new JcrDeploymentDescriptorProject(n);
    }

    public JcrDeploymentDescriptorProject(Node node) throws RepositoryException {
        super(node);

        projects = new HashMap<String, RProjectDescriptor>();

        NodeIterator ni = node.getNodes();
        while (ni.hasNext()) {
            Node n = ni.nextNode();

            JcrProjectDescriptor pd = new JcrProjectDescriptor(n);
            projects.put(pd.getProjectName(), pd);
        }
    }

    public RProjectDescriptor createProjectDescriptor(String name) throws RRepositoryException {
        Node node = node();
        try {
            return JcrProjectDescriptor.create(node, name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot create project descriptor ''{0}''.", e, name);
        }
    }

    public Collection<RProjectDescriptor> getProjectDescriptors() {
        return projects.values();
    }

    public RDeploymentDescriptorProject getProjectVersion(CommonVersion version) throws RRepositoryException {
        try {
            Node frozenNode = NodeUtil.getNode4Version(node(), version);
            return new JcrOldDeploymentProject(getName(), frozenNode);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot get project version.", e);
        }
    }

    public void setProjectDescriptors(Collection<RProjectDescriptor> projectDescriptors) throws RRepositoryException {
        try {
            Node node = node();
            NodeUtil.smartCheckout(node, false);
            NodeIterator ni = node.getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();

                n.remove();
            }
            HashMap<String, RProjectDescriptor> newProjects = new HashMap<String, RProjectDescriptor>();
            for (RProjectDescriptor pd : projectDescriptors) {
                JcrProjectDescriptor newPD = JcrProjectDescriptor.create(node, pd.getProjectName());
                newPD.setProjectVersion(pd.getProjectVersion());

                newProjects.put(newPD.getProjectName(), newPD);
            }
            projects.clear();
            projects = newProjects;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set project descriptors for ''{0}''.", e, getName());
        }
    }

    // --- protected
}
