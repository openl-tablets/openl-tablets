package org.openl.rules.webstudio.web.repository.tree;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProjectArtefact;

/**
 * @author Andrei Astrouski
 */
public interface TreeNode extends org.richfaces.model.TreeNode {

    // To sort elements by adding prefix in id: folder before files.
    String FOLDER_PREFIX = "dir_";
    String FILE_PREFIX = "file_";

    Map<Object, TreeNode> getElements();

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
    TreeNode add(TreeNode child);

    void removeChildren();

    /**
     * Clears children. Works recursively.
     */
    void clear();

    List<TreeNode> getChildNodes();

    AProjectArtefact getData();

    void setData(AProjectArtefact data);

    String getId();

    /**
     * Returns display name of the node.
     *
     * @return name of node in tree
     */
    String getName();

    TreeNode getParent();

    void setParent(TreeNode parent);

    String getVersionName();

    Collection<ProjectVersion> getVersions();

    boolean hasVersions();

    boolean isLeafOnly();

    void refresh();

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
    String getType();

}
