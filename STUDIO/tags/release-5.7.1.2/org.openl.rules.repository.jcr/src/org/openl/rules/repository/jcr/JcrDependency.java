package org.openl.rules.repository.jcr;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.repository.RDependency;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrDependency extends JcrCommonArtefact implements RDependency {

    private CommonVersion lowVersion;
    private CommonVersion highVersion;

    protected static JcrDependency createDependency(Node parentNode, String nodeName, CommonVersion lowVersion,
            CommonVersion highVersion) throws RepositoryException {
        Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_DEPENDENCY, false);

        if (lowVersion != null) {
            n.setProperty("lowVersion", lowVersion.getVersionName());
        }
        if (highVersion != null) {
            n.setProperty("highVersion", highVersion.getVersionName());
        }

        parentNode.save();
        n.save();

        return new JcrDependency(n);
    }

    protected JcrDependency(Node node) throws RepositoryException {
        super(node);

        // can be frozen node too
        // checkNodeType(JcrNT.NT_DEPENDENCY);

        lowVersion = wrapProp("lowVersion");
        highVersion = wrapProp("highVersion");
    }

    @Override
    public RVersion getActiveVersion() {
        // not supported
        return null;
    }

    public CommonVersion getLowerLimit() {
        return lowVersion;
    }

    public String getProjectName() {
        return getName();
    }

    public CommonVersion getUpperLimit() {
        return highVersion;
    }

    @Override
    public List<RVersion> getVersionHistory() throws RRepositoryException {
        throw new RRepositoryException("Not supported!", null);
    }

    public boolean hasUpperLimit() {
        return (highVersion != null);
    }

    // --- protected

    protected CommonVersion wrapProp(String propName) throws RepositoryException {
        if (!node().hasProperty(propName)) {
            return null;
        }

        String s = node().getProperty(propName).getString();
        return new CommonVersionImpl(s);
    }
}
