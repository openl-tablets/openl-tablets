package org.openl.rules.ui.tree;

import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;
import org.openl.util.conf.Version;

/**
 * Folder tree node that represents table with several versions. If node has
 * several versions then folder node will be linked with one of its child. If
 * node has only one version then it will be leaf in tree.
 * 
 * Linked child is child with "active" table(there is only one "active" table
 * among all version of the table) or the child with greatest version.
 * 
 * @author PUdalau
 */
public class VersionedTreeNode extends ProjectTreeNode {
    
    private static Log LOG = LogFactory.getLog(VersionedTreeNode.class);
    
    private TableSyntaxNode linkedChild;

    public VersionedTreeNode(String[] displayName, TableSyntaxNode table) {
        super(displayName, IProjectTypes.PT_TABLE + "." + table.getType(), null, null, 0, null);
    }

    @Override
    public String getType() {
        return IProjectTypes.PT_TABLE + "." + linkedChild.getType();
    }

    @Override
    public String getUri() {
        return linkedChild.getUri();
    }

    @Override
    public Object getProblems() {
        return linkedChild.getErrors() != null ? linkedChild.getErrors() : linkedChild.getValidationResult();
    }

    @Override
    public boolean hasProblems() {
        return getProblems() != null;
    }

    @Override
    public TableSyntaxNode getTableSyntaxNode() {
        return linkedChild;
    }

    @Override
    public TreeMap<Object, ITreeNode<Object>> getElements() {
        TreeMap<Object, ITreeNode<Object>> elements = super.getElements();
        if (elements.size() < 2) {
            return new TreeMap<Object, ITreeNode<Object>>();
        }
        return elements;
    }

    @Override
    public boolean isLeaf() {
        return getElements().size() < 1;
    }

    @Override
    public Iterator<ITreeNode<Object>> getChildren() {
        return getElements().values().iterator();
    }

    @Override
    public void addChild(Object key, ITreeNode<Object> child) {
        super.addChild(key, child);
        ProjectTreeNode newChild = (ProjectTreeNode) child;
        if (linkedChild == null) {
            linkedChild = newChild.getTableSyntaxNode();
        } else {
            if (findLaterTable(linkedChild, newChild.getTableSyntaxNode()) > 0) {
                linkedChild = newChild.getTableSyntaxNode();
            }
        }
    }

    /**
     * Finds table with biggest version(it will later table) or "active" table;
     * 
     * @return -1 if the first later or "active", 1 if second later "active" and
     *         0 if tables are "inactive" and have similar versions
     */
    public static int findLaterTable(TableSyntaxNode first, TableSyntaxNode second) {
        // Not all the tables have the property 'active'. e.g. it is more common case when Property table component 
        // doesn`t have this property. So we need to check if the property exists. 
        // author: DLiauchuk
        if (first.getTableProperties().getActive() != null) {
            if (first.getTableProperties().getActive()) {
                return -1;
            } else if (second.getTableProperties().getActive()) {
                return 1;
            }
        } else {
            return 0;
        }
        try {
            Version firstNodeVersion = Version.parseVersion(first.getTableProperties().getVersion(), 0, "..");
            Version secondNodeVersion = Version.parseVersion(second.getTableProperties().getVersion(), 0, "..");
            return secondNodeVersion.compareTo(firstNodeVersion);
        } catch (RuntimeException e) {
            // it is just fix to avoid tree crashing.
            // we need to validate format of the versions, during compilation of Openl and also on UI.
            LOG.error(e);
        }
        return 0;
        
    }
}
