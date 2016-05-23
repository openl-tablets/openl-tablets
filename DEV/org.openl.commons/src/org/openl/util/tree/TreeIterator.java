/*
 * Created on May 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.tree;

import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;

import org.openl.util.AOpenIterator;

/**
 * @author snshor
 * 
 */
public class TreeIterator<N> extends AOpenIterator<N> {

    static final class NodeInfo<N> {
        N node;

        Iterator<N> children;

        NodeInfo(N node, Iterator<N> children) {
            this.node = node;
            this.children = children;
        }
    }

    public interface TreeAdaptor<N> {
        /**
         * 
         * @param node parent node
         * @param mode inthis case only, left-right or right-left is relevant
         * @return iterator over children collection, null or empty iterator if
         *         none
         */
        Iterator<N> children(N node);
    }

    public static final int DEFAULT = 0, // top-bottom, left-right, all nodes
                                         // included
            LEAVES_ONLY = 1, RIGHT_TO_LEFT = 2, NO_LEAVES = 4, BOTTOM_TOP = 8; // iterate
                                                                               // over
                                                                               // children
                                                                               // first

    N currentNode;
    TreeAdaptor<N> adaptor;

    Stack<NodeInfo<N>> path = new Stack<NodeInfo<N>>();

    int mode = 0;

    Iterator<N> children = null;

    public TreeIterator(N treeRoot, TreeAdaptor<N> adaptor, int mode) {
        this.children = Collections.singletonList(treeRoot).iterator();
        this.adaptor = adaptor;
        this.mode = mode;
        findNextNode();
    }

    private void findNextNode() {
        if (children.hasNext()) {
            N nextChild = children.next();

            Iterator<N> grandChildren = adaptor.children(nextChild);

            if (isEmpty(grandChildren)) // nextChild is a leaf
            {
                currentNode = nextChild;
                return;
            }

            if ((mode & RIGHT_TO_LEFT) != 0) {
                grandChildren = reverse(grandChildren);
            }

            path.push(new NodeInfo<N>(nextChild, children));
            children = grandChildren;

            if ((mode & BOTTOM_TOP) != 0)// children first
            {
                findNextNode();
            } else {
                currentNode = nextChild;
            }
            return;
        }

        // if children don't have next

        if (path.size() == 0) {
            currentNode = null;
            return;
        }

        NodeInfo<N> info = path.pop();
        children = info.children;

        if ((mode & BOTTOM_TOP) != 0)// children first
        {
            currentNode = info.node;
            return;
        }

        findNextNode();

    }

    public boolean hasNext() {
        return currentNode != null;
    }

    public N next() {
        N result = currentNode;
        findNextNode();

        return result;
    }

    public TreeAdaptor<N> getAdaptor() {
        return adaptor;
    }

    public void setAdaptor(TreeAdaptor<N> adaptor) {
        this.adaptor = adaptor;
    }
}
