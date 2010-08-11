/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import java.util.Iterator;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.AIndexedIterator;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInterval;
import org.openl.util.tree.TreeIterator;

/**
 * @author snshor
 */
public abstract class ASyntaxNode implements ISyntaxNode {
    
    public static final TreeIterator.TreeAdaptor<ISyntaxNode> TREE_ADAPTOR = new SyntaxTreeAdaptor();
    
    private String type;

    private IOpenSourceCodeModule module;

    private ISyntaxNode parent;

    private ILocation location;    

    private static void printSpace(int level, StringBuffer buf) {
        for (int j = 0; j < level; j++) {
            buf.append("  ");
        }
    }

    public ASyntaxNode(String type, ILocation location,
    // Map<String, String> properties,
            IOpenSourceCodeModule module) {
        this.type = type;
        this.location = location;
        // this.properties = properties;
        this.module = module;
        // this.namespace = namespace;
    }

    public ILocation getLocation() {
        return location;
    }

    public IOpenSourceCodeModule getModule() {
        if (module != null) {
            return module;
        }
        if (parent != null) {
            return parent.getModule();
        }
        return null;
    }

    public ISyntaxNode getParent() {
        return parent;
    }

    public ILocation getSourceLocation() {
        if (location == null) {
            int n = getNumberOfChildren();
            switch (n) {
                case 0:
                    return null;
                case 1:
                    return getChild(0).getSourceLocation();
                default:
                    return new TextInterval(getChild(0).getSourceLocation().getStart(), getChild(n - 1)
                            .getSourceLocation().getEnd());
            }
        }
        return location;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.parser.SyntaxNode#getType()
     */
    public String getType() {
        return type;
    }

    public void print(int level, StringBuffer buf) {
        int nkids = getNumberOfChildren();

        printMySelf(level, buf);
        buf.append('\n');
        for (int i = 0; i < nkids; i++) {
            ISyntaxNode ch = getChild(i);
            if (ch == null) {
                printSpace(level + 1, buf);
                buf.append("null\n");
            } else {
                ch.print(level + 1, buf);
            }
        }
    }

    protected void printMySelf(int level, StringBuffer buf) {
        printSpace(level, buf);
        buf.append(getType());

    }

    public void setParent(ISyntaxNode parent) {
        this.parent = parent;
    }
    public void setType(String string) {
        type = string;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        print(0, buf);
        return buf.toString();
    }
    
    private static class SyntaxTreeAdaptor implements TreeIterator.TreeAdaptor<ISyntaxNode> {
        private static class SyntaxNodeChildrenIterator extends AIndexedIterator<ISyntaxNode> {
            private ISyntaxNode node;

            public SyntaxNodeChildrenIterator(ISyntaxNode node) {
                super(0, node.getNumberOfChildren(), 1);
                this.node = node;
            }

            /*
             * (non-Javadoc)
             *
             * @see org.openl.util.AIndexedIterator#indexedElement(int)
             */
            @Override
            protected ISyntaxNode indexedElement(int i) {
                return node.getChild(i);
            }

        }

        public Iterator<ISyntaxNode> children(ISyntaxNode syntaxNode) {
            return new SyntaxNodeChildrenIterator(syntaxNode);
        }

    }

}
