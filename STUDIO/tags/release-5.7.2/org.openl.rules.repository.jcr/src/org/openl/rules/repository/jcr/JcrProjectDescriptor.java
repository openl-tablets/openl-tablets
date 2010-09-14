package org.openl.rules.repository.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrProjectDescriptor implements RProjectDescriptor {
    private Node node;

    private String name;
    private JcrVersion version;

    protected static JcrProjectDescriptor create(Node parentNode, String projectName) throws RepositoryException {
        Node n = NodeUtil.createNode(parentNode, projectName, JcrNT.NT_PROJECT_DESCRIPTOR, false);

        parentNode.save();

        return new JcrProjectDescriptor(n);
    }

    protected JcrProjectDescriptor(Node node) throws RepositoryException {
        this.node = node;

        name = node.getName();

        version = new JcrVersion(node);
    }

    public String getProjectName() {
        return name;
    }

    public RVersion getProjectVersion() {
        return version;
    }

    protected Node node() {
        return node;
    }

    public void setProjectVersion(RVersion version) throws RRepositoryException {
        Node n = node();

        this.version = new JcrVersion(version);
        try {
            this.version.updateVersion(n);
            this.version.updateRevision(n);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set project version.", e);
        }
    }
}
