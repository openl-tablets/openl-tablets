package org.openl.rules.repository.jcr;

import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import java.util.LinkedList;
import java.util.List;

public class JcrCommonArtefact {
    private final Logger log = LoggerFactory.getLogger(JcrCommonArtefact.class);

    private String name;
    private Node node;
    private boolean oldVersion;

    protected JcrCommonArtefact(Node node, String name, boolean oldVersion) {
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
            Node parent = n.getParent();
            NodeUtil.smartCheckout(n, true);

            n.remove();

            parent.save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Delete.", e);
        }
    }

    public RVersion getActiveVersion() {
        try {
            if (oldVersion || node.isNew()) {
                RVersion result = new JcrVersion(node);
                return result;
            } else {
                Version v = node().getBaseVersion();
                RVersion result = new JcrVersion(v);
                return result;
            }
        } catch (RepositoryException e) {
            log.error("getActiveVersion", e);
            return RVersion.NON_DEFINED_VERSION;
        }
    }

    public String getName() {
        return name;
    }

    public List<RVersion> getVersionHistory() throws RRepositoryException {
        try {
            VersionHistory vh = node().getVersionHistory();
            VersionIterator vi = vh.getAllVersions();
            LinkedList<RVersion> result = new LinkedList<>();
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
