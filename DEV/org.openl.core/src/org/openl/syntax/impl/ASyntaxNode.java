/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 */
public abstract class ASyntaxNode implements ISyntaxNode {

    private String type;

    private IOpenSourceCodeModule module;

    private ISyntaxNode parent;

    private ILocation location;

    public ASyntaxNode(String type,
            ILocation location,
            IOpenSourceCodeModule module) {
        this.type = type;
        this.location = location;
        this.module = module;
    }

    private static void printSpace(int level, StringBuilder buf) {
        for (int j = 0; j < level; j++) {
            buf.append("  ");
        }
    }

    public ILocation getLocation() {
        return location;
    }

    @Override
    public IOpenSourceCodeModule getModule() {
        if (module != null) {
            return module;
        }
        if (parent != null) {
            return parent.getModule();
        }
        return null;
    }

    @Override
    public ISyntaxNode getParent() {
        return parent;
    }

    @Override
    public void setParent(ISyntaxNode parent) {
        this.parent = parent;
    }

    @Override
    public ILocation getSourceLocation() {
        if (location == null) {
            int n = getNumberOfChildren();
            switch (n) {
                case 0:
                    return null;
                case 1:
                    return getChild(0).getSourceLocation();
                default:
                    ILocation startLocation = getChild(0).getSourceLocation();
                    if (startLocation == null) {
                        return null;
                    }
                    ILocation endLocation = getChild(n - 1).getSourceLocation();
                    if (endLocation == null) {
                        return null;
                    }
                    return new TextInterval(startLocation.getStart(), endLocation.getEnd());
            }
        }
        return location;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.parser.SyntaxNode#getType()
     */
    @Override
    public String getType() {
        return type;
    }

    public void setType(String string) {
        type = string;
    }

    @Override
    public void print(int level, StringBuilder buf) {
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

    protected void printMySelf(int level, StringBuilder buf) {
        printSpace(level, buf);
        buf.append(getType());

    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        print(0, buf);
        return buf.toString();
    }
}
