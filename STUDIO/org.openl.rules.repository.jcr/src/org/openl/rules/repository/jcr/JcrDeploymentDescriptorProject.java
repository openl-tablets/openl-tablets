package org.openl.rules.repository.jcr;

import java.util.Collection;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.RDeploymentDescriptorProject;
import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.exceptions.RModifyException;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrDeploymentDescriptorProject extends JcrCommonProject implements RDeploymentDescriptorProject {
    private HashMap<String, RProjectDescriptor> projects;

    protected static JcrDeploymentDescriptorProject createProject(Node parentNode, String name) throws RepositoryException {
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
    
    public Collection<RProjectDescriptor> getProjectDescriptors() {
        return projects.values();
    }
    
    public void setProjectDescriptors(Collection<RProjectDescriptor> projectDescriptors) throws RModifyException {
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
                JcrProjectDescriptor newPD = JcrProjectDescriptor.create(node,
                        pd.getProjectName());
                newPD.setProjectVersion(pd.getProjectVersion());

                newProjects.put(newPD.getProjectName(), newPD);
            }
            projects.clear();
            projects = newProjects;
        } catch (RepositoryException e) {
            throw new RModifyException("Cannot set project descriptors for {0}", e, getName());
        }        
    }
    
    public RProjectDescriptor createProjectDescriptor(String name) throws RRepositoryException {
        Node node = node();
        try {
            return JcrProjectDescriptor.create(node, name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot create project descriptor {0}", e, name);
        }        
    }
    
    // --- protected
}
