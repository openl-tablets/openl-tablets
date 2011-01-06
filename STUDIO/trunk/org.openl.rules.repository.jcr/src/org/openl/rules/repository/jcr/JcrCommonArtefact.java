package org.openl.rules.repository.jcr;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrCommonArtefact {
    private final static Log log = LogFactory.getLog(JcrCommonArtefact.class);

    private String name;
    private Node node;
    private boolean oldVersion;

    protected JcrCommonArtefact(Node node, String name, boolean oldVersion) throws RepositoryException {
        this.node = node;
        this.name = name;
        this.oldVersion = oldVersion;
    }

    protected JcrCommonArtefact(Node node, String name) throws RepositoryException {
        this(node, name, false);
    }

    protected JcrCommonArtefact(Node node) throws RepositoryException {
        this(node, node.getName());
    }

    public boolean isOldVersion() {
        return oldVersion;
    }

    public void delete() throws RRepositoryException {
        try {
            Node n = node();

            NodeUtil.smartCheckout(n, true);

            n.remove();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Delete.", e);
        }
    }

    public RVersion getActiveVersion() {
        try {
            if (oldVersion) {
                RVersion result = new JcrVersion(node);
                return result;
            } else {
                Version v = node().getBaseVersion();
                RVersion result = new JcrVersion(v);
                return result;
            }
        } catch (RepositoryException e) {
            log.info("getActiveVersion", e);
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public List<RVersion> getVersionHistory() throws RRepositoryException {
        try {
            VersionHistory vh = node().getVersionHistory();
            VersionIterator vi = vh.getAllVersions();
            LinkedList<RVersion> result = new LinkedList<RVersion>();
            while (vi.hasNext()) {
                Version v = vi.nextVersion();

                if (NodeUtil.isRootVersion(v)) {
                    // TODO Shall we add first (0) version? (It is marker like,
                    // no real values)
                } else {
                    JcrVersion jvi = new JcrVersion(v);
                    result.add(jvi);
                }
            }
            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Version History.", e);
        }
    }

    public Node node() {
        return node;
    }
}
