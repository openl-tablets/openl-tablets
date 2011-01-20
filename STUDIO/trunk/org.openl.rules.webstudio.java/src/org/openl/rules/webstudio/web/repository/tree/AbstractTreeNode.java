package org.openl.rules.webstudio.web.repository.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.webstudio.web.repository.DependencyBean;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.richfaces.model.TreeNode;

/**
 * This abstract class implements basic functionality of {@link TreeNode}
 * interface. Every particular tree node should extends this class and adds own
 * implementation for UI related methods: {@link #getType()},
 * {@link #getIcon()}, and {@link #getIconLeaf()}.
 *
 * <p>
 * Derived classes is going to be used in richfaces:
 *
 * <pre>
 * &lt;rich:tree value=&quot;...&quot; var=&quot;item&quot; nodeFace=&quot;#{item.type}&quot; &gt;
 *     &lt;rich:treeNode type=&quot;type1&quot; icon=&quot;#{item.icon}&quot; iconLeaf=&quot;#{item.iconLeaf}&quot;&gt;
 *         &lt;h:outputText value=&quot;#{item.name}&quot; /&gt;
 *     &lt;/rich:treeNode&gt;
 *     &lt;rich:treeNode type=&quot;type2&quot; icon=&quot;#{item.icon}&quot; iconLeaf=&quot;#{item.iconLeaf}&quot;&gt;
 *         &lt;h:outputText value=&quot;#{item.name}&quot; /&gt;
 *     &lt;/rich:treeNode&gt;
 *
 *     ... more node types
 *
 * &lt;/rich:tree&gt;
 * </pre>
 *
 * @see TreeNode
 *
 * @author Aleh Bykhavets
 *
 */
public abstract class AbstractTreeNode implements TreeNode<AProjectArtefact> {

    private static final long serialVersionUID = 1238954077308840345L;

    /**
     * Empty collection. It is used in LeafOnly mode to return empty iterator.
     *
     * Must be the same type as {@link #elements} is.
     */
    private static final transient Map EMPTY = new LinkedHashMap();

    // private static final Comparator<AbstractTreeNode> CHILD_COMPARATOR
    // = new Comparator<AbstractTreeNode>() {
    // public int compare(AbstractTreeNode o1, AbstractTreeNode o2) {
    // ProjectArtefact p1 = o1.dataBean;
    // ProjectArtefact p2 = o2.dataBean;
    //
    // if (p1 == null || p2 == null || p1.isFolder() == p2.isFolder()) {
    // return o1.getName().compareToIgnoreCase(o2.getName());
    // }
    //
    // return (p1.isFolder() ? -1 : 1);
    // }
    // };
    //
    /**
     * Identifier of the node.
     */
    private String id;
    /**
     * Display name
     */
    private String name;

    /**
     * Reference on parent node. For upper level node(s) it is <code>null</code>
     */
    private TreeNode<AProjectArtefact> parent;
    /**
     * Collection of children. In LeafOnly mode it is left uninitialized.
     */
    private Map<Object, AbstractTreeNode> elements;

    /**
     * When <code>true</code> then the node cannot have children. Any
     * operation that implies adding/getting/removing children will lead to
     * {@link UnsupportedOperationException}.
     */
    private boolean isLeafOnly;

    private AProjectArtefact data;

    /**
     * Creates tree node that can have children.
     *
     * @param id id to distinguish the node among others
     * @param name display name of the node
     */
    public AbstractTreeNode(String id, String name) {
        this(id, name, false);
    }

    /**
     * Creates tree node. Can control whether the node is LeafOnly (i.e. cannot
     * have children) or usual one.
     *
     * @param id id to distinguish the node among others
     * @param name display name of the node
     * @param isLeafOnly whether the node is LeafOnly (true) or usual (false)
     */
    public AbstractTreeNode(String id, String name, boolean isLeafOnly) {
        this.id = id;
        this.name = name;
        this.isLeafOnly = isLeafOnly;

        if (!isLeafOnly) {
            elements = new LinkedHashMap<Object, AbstractTreeNode>();
        }
    }

    /**
     * Short for <code>addChild(child.getId(), child)</code>.
     * <p>
     * Can be use in a chain:
     *
     * <pre>
     * add(child1).add(child2)...add(childN);
     * </pre>
     *
     * @param child a node to be added as child
     * @return self-reference on the node
     */
    public AbstractTreeNode add(AbstractTreeNode child) {
        addChild(child.getId(), child);
        return this;
    }

    /**
     * @see TreeNode#addChild(Object, TreeNode)
     */
    public void addChild(Object id, TreeNode<AProjectArtefact> child) {
        checkLeafOnly();

        elements.put(id, (AbstractTreeNode) child);
        child.setParent(this);
    }

    /**
     * When the node is in LeafOnly mode, i.e. cannot have children, this method
     * will throw exception.
     * <p>
     * This method is used internally in {@link #addChild(Object, TreeNode)},
     * {@link #getChild(Object)}, and {@link #removeChild(Object)} methods.
     * <p>
     * Note, that method {@link #getChildren()} must work in any case.
     *
     * @throws UnsupportedOperationException if the node is LeafOnly and current
     *             operation implies work with children
     */
    private void checkLeafOnly() {
        if (isLeafOnly) {
            throw new UnsupportedOperationException("cannot have children");
        }
    }

    /**
     * Clears children. Works recursively.
     */
    protected void clear() {
        if (elements != null) {
            // recursion
            for (AbstractTreeNode child : elements.values()) {
                child.clear();
            }

            elements.clear();
        }
    }

    /**
     * @see TreeNode#getChild(Object)
     */
    public AbstractTreeNode getChild(Object id) {
        checkLeafOnly();

        return elements.get(id);
    }

    public List<AbstractTreeNode> getChildNodes() {
        List<AbstractTreeNode> list = new ArrayList<AbstractTreeNode>(elements.values());
        // elements are sorted already
        // Collections.sort(list, CHILD_COMPARATOR);

        return list;
    }

    /**
     * @see TreeNode#getChildren()
     */
    public Iterator getChildren() {
        Iterator result;

        if (isLeafOnly) {
            checkLeafOnly();
            // trick: return iterator for empty set
            result = EMPTY.entrySet().iterator();
            // work around limitation for TreeNode
        } else {
            result = elements.entrySet().iterator();
        }

        return result;
    }

    public AProjectArtefact getData() {
        return data;
    }

    public List<DependencyBean> getDependencies() {
        return null;
    }

    /**
     * Returns URL of image that will be displayed with the node when it has at
     * least one child.
     *
     * <pre>
     * &lt;rich:treeNode &lt;b&gt;icon=&quot;#{item.icon}&quot;&lt;/b&gt; ... &gt;
     * </pre>
     *
     * @return URL of image
     */
    public abstract String getIcon();

    /**
     * Returns URL of image that will be displayed with the node when
     * {@link #isLeaf()} is true.
     *
     * <pre>
     * &lt;rich:treeNode &lt;b&gt;iconLeaf=&quot;#{item.iconLeaf}&quot;&lt;/b&gt; ... &gt;
     * </pre>
     *
     * @return URL of image
     */
    public abstract String getIconLeaf();

    // ------ * ------

    public String getId() {
        return id;
    }

    /**
     * Returns display name of the node.
     *
     * @return name of node in tree
     */
    public String getName() {
        return name;
    }

    /**
     * @see TreeNode#getParent()
     */
    public TreeNode<AProjectArtefact> getParent() {
        return parent;
    }

    /**
     * Returns type of the node in tree.
     *
     * <pre>
     * &lt;rich:tree var=&quot;item&quot; &lt;b&gt;nodeFace=&quot;#{item.type}&quot;&lt;/b&gt; ... &gt;
     *     &lt;rich:treeNode &lt;b&gt;type=&quot;project&quot;&lt;/b&gt; ... &gt;
     * </pre>
     *
     * @return type of node
     */
    public abstract String getType();

    public String getVersionName() {
        if (data instanceof AProject) {
            ProjectVersion version = ((AProject) data).getVersion();
            return (version == null) ? null : version.getVersionName();
        }
        return null;
    }

    private UserWorkspaceProject findProjectContainingCurrentArtefact() {
        TreeNode<AProjectArtefact> node = this;
        while (node != null && !(node.getData() instanceof UserWorkspaceProject)) {
            node = node.getParent();
        }
        return node == null ? null : (UserWorkspaceProject) node.getData();
    }

    public Collection<ProjectVersion> getVersions() {
        if (data instanceof AProjectArtefact) {
            UserWorkspaceProject project = findProjectContainingCurrentArtefact();
            List<ProjectVersion> result;
            if (project != null) {
                result = project.getVersionsForArtefact((getData()).getArtefactPath().withoutFirstSegment());
            } else {
                result = getData().getVersions();
            }
            Collections.sort(result, RepositoryUtils.VERSIONS_REVERSE_COMPARATOR);
            return result;
        } else {
            return new LinkedList<ProjectVersion>();
        }
    }

    /**
     * @see TreeNode#isLeaf()
     */
    public boolean isLeaf() {
        boolean result;

        if (isLeafOnly) {
            result = true;
        } else {
            result = elements.isEmpty();
        }

        return result;
    }

    // ------ UI methods ------

    /**
     * @see TreeNode#removeChild(Object)
     */
    public void removeChild(Object id) {
        checkLeafOnly();

        elements.remove(id);
    }

    public void removeChildren() {
        checkLeafOnly();
        elements.clear();
    }

    public void setData(AProjectArtefact data) {
        this.data = data;
    }

    /**
     * @see TreeNode#setParent(TreeNode)
     */
    public void setParent(TreeNode<AProjectArtefact> parent) {
        this.parent = parent;
    }
    
    public void refresh(){
        data.refresh();
    }
}
