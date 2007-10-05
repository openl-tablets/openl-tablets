package org.openl.rules.ui.repository.tree;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.richfaces.model.TreeNode;

/**
 * This abstract class implements basic functionality of {@link TreeNode} interface.
 * Every particular tree node should extends this class
 * and adds own implementation for UI related methods:
 * {@link #getType()}, {@link #getIcon()}, and {@link #getIconLeaf()}.
 *
 * <p>
 * Derived classes is going to be used in richfaces:
 * <pre>
 * &lt;rich:tree value="..." var="item" nodeFace="#{item.type}" >
 *     &lt;rich:treeNode type="type1" icon="#{item.icon}" iconLeaf="#{item.iconLeaf}">
 *         &lt;h:outputText value="#{item.name}" />
 *     &lt;/rich:treeNode>
 *     &lt;rich:treeNode type="type2" icon="#{item.icon}" iconLeaf="#{item.iconLeaf}">
 *         &lt;h:outputText value="#{item.name}" />
 *     &lt;/rich:treeNode>
 *
 *     ... more node types
 *
 * &lt;/rich:tree>
 * </pre>
 *
 * @see TreeNode
 *
 * @author Aleh Bykhavets
 *
 */
public abstract class AbstractTreeNode implements TreeNode {

    private static final long serialVersionUID = 1238954077308840345L;

    /**
     * Empty collection.  It is used in LeafOnly mode to return empty iterator.
     * 
     * Must be the same type as {@link #elements} is.
     */
    private static final transient Map EMPTY = new LinkedHashMap();

    /**
     * Identifier of the node.
     */
    private long id;
    /**
     * Display name
     */
    private String name;

    /**
     * Reference on parent node.  For upper level node(s) it is <code>null</code>
     */
    private TreeNode parent;
    /**
     * Collection of children.  In LeafOnly mode it is left uninitialized.
     */
    private Map <Object, AbstractTreeNode> elements;

    /**
     * When <code>true</code> then the node cannot have children.
     * Any operation that implies adding/getting/removing children 
     * will lead to {@link UnsupportedOperationException}.
     */
    private boolean isLeafOnly;

    private Object dataBean;

    /**
     * Creates tree node that can have children.
     *
     * @param id   id to distinguish the node among others
     * @param name display name of the node
     */
    public AbstractTreeNode(long id, String name) {
        this(id, name, false);
    }

    /**
     * Creates tree node.  Can control whether the node is LeafOnly
     * (i.e. cannot have children) or usual one.
     * 
     * @param id   id to distinguish the node among others
     * @param name display name of the node
     * @param isLeafOnly whether the node is LeafOnly (true) or usual (false)
     */
    public AbstractTreeNode(long id, String name, boolean isLeafOnly) {
        this.id = id;
        this.name = name;
        this.isLeafOnly = isLeafOnly;

        if (!isLeafOnly) {
            elements = new LinkedHashMap<Object, AbstractTreeNode>();
        }
    }

    /**
     * When the node is in LeafOnly mode, i.e. cannot have children,
     * this method will throw exception.
     * <p>
     * This method is used internally in {@link #addChild(Object, TreeNode)}, 
     * {@link #getChild(Object)}, and {@link #removeChild(Object)} methods.
     * <p>
     * Note, that method {@link #getChildren()} must work in any case.
     * 
     * @throws UnsupportedOperationException if the node is LeafOnly and current operation implies work with children
     */
    private void checkLeafOnly() {
        if (isLeafOnly) {
            throw new UnsupportedOperationException("cannot have children");
        }
    }

    /**
     * @see TreeNode#addChild(Object, TreeNode)
     */
    public void addChild(Object id, TreeNode child) {
        checkLeafOnly();

        elements.put(id, (AbstractTreeNode)child);
        child.setParent(this);
    }

    /**
     * @see TreeNode#getChild(Object)
     */
    public TreeNode getChild(Object id) {
        checkLeafOnly();

        return elements.get(id);
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

    /**
     * @see TreeNode#getData()
     */
    public Object getData() {
        return this;
    }

    /**
     * @see TreeNode#getParent()
     */
    public TreeNode getParent() {
        return parent;
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

    /**
     * @see TreeNode#removeChild(Object)
     */
    public void removeChild(Object id) {
        checkLeafOnly();

        elements.remove(id);
    }

    /**
     * @see TreeNode#setData(Object)
     */
    public void setData(Object data) {
        // do nothing
    }

    /**
     * @see TreeNode#setParent(TreeNode)
     */
    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    // ------ * ------

    public long getId() {
        return id;
    }

    public Object getDataBean() {
        return dataBean;
    }

    public void setDataBean(Object dataBean) {
        this.dataBean = dataBean;
    }

    /**
     * Short for <code>addChild(child.getId(), child)</code>.
     * <p>
     * Can be use in a chain:
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
     * Clears children.  Works recursively.
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

    // ------ UI methods ------

    /**
     * Returns display name of the node.
     *
     * @return name of node in tree
     */
    public String getName() {
        return name;
    }

    /**
     * Returns type of the node in tree.
     * <pre>
     * &lt;rich:tree var="item" <b>nodeFace="#{item.type}"</b> ... >
     *     &lt;rich:treeNode <b>type="project"</b> ... >
     * </pre>
     * 
     * @return type of node
     */
    public abstract String getType();

    /**
     * Returns URL of image that will be displayed with the node
     * when it has at least one child.
     * <pre>
     * &lt;rich:treeNode <b>icon="#{item.icon}"</b> ... >
     * </pre>
     * 
     * @return URL of image
     */
    public abstract String getIcon();

    /**
     * Returns URL of image that will be displayed with the node
     * when {@link #isLeaf()} is true.
     * <pre>
     * &lt;rich:treeNode <b>iconLeaf="#{item.iconLeaf}"</b> ... >
     * </pre>
     * 
     * @return URL of image
     */
    public abstract String getIconLeaf();
}
