package org.openl.rules.repository.jcr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.RDependency;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrDependencies extends JcrCommonArtefact {

    protected JcrDependencies(Node node) throws RepositoryException {
        super(node);

        // can be frozen node too
        // checkNodeType(JcrNT.NT_DEPENDENCIES);
    }

    @Override
    public RVersion getActiveVersion() {
        // not supported
        return null;
    }

    public Collection<RDependency> getDependencies() throws RRepositoryException {
        LinkedList<RDependency> result = new LinkedList<RDependency>();

        try {
            NodeIterator ni = node().getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();

                result.add(new JcrDependency(n));
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot get dependencies.", e);
        }

        return result;
    }

    @Override
    public List<RVersion> getVersionHistory() throws RRepositoryException {
        throw new RRepositoryException("Not supported!", null);
    }

    public void updateDependencies(Collection<? extends RDependency> dependencies) throws RRepositoryException {
        try {
            NodeUtil.smartCheckout(node(), true);

            // 1. clear
            NodeIterator ni = node().getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();
                n.remove();
            }

            node().save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot reset dependencies.", e);
        }

        try {
            // 2. create new
            for (RDependency dep : dependencies) {
                JcrDependency.createDependency(node(), dep.getProjectName(), dep.getLowerLimit(), dep.getUpperLimit());
            }

            node().save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set dependencies.", e);
        }
    }
}
