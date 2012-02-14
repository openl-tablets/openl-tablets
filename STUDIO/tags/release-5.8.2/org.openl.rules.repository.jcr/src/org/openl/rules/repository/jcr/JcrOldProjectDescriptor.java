package org.openl.rules.repository.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrOldProjectDescriptor implements RProjectDescriptor {
    // private Node node;

    private String name;
    private JcrVersion version;

    protected JcrOldProjectDescriptor(Node node) throws RepositoryException {
        // this.node = node;

        name = node.getName();

        version = new JcrVersion(node);
    }

    public String getProjectName() {
        return name;
    }

    public RVersion getProjectVersion() {
        return version;
    }

    protected void notSupported() throws RRepositoryException {
        throw new RRepositoryException("Cannot modify artefact version!", null);
    }

    // --- protected

    public void setProjectVersion(RVersion version) throws RRepositoryException {
        notSupported();
    }
}
