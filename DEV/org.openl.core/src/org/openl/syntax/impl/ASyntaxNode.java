/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 */
public abstract class ASyntaxNode implements ISyntaxNode {

    private String type;

    private final IOpenSourceCodeModule module;

    private ISyntaxNode parent;

    private final ILocation location;

    public ASyntaxNode(String type, ILocation location, IOpenSourceCodeModule module) {
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

    /**
     * Returns the source location of this syntax node.
     *
     * <p>If the node's location is not set, this method computes the source location from its children.
     * If there are no children, it returns null. For a single child, it returns that child's source location.
     * For multiple children, if both the first and last child's source locations are defined, it returns a new
     * location spanning from the start of the first child's location to the end of the last child's location;
     * otherwise, it returns null.</p>
     *
     * @return the computed source location, or {@code null} if it cannot be determined
     */
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

    /**
     * Creates a sub-module that represents the segment of source code corresponding to this node.
     *
     * <p>This method retrieves the current module and its code, then calculates the absolute start
     * and end positions from the node's source location (with the end position being exclusive) to
     * construct a new {@code SubTextSourceCodeModule} encapsulating the relevant portion of the source.
     *
     * @return a source code module corresponding to the node's source location
     */
    @Override
    public IOpenSourceCodeModule getSourceCodeModule() {
        var module = getModule();
        var info = new TextInfo(module.getCode());
        var location = getSourceLocation();
        return new SubTextSourceCodeModule(module,
                location.getStart().getAbsolutePosition(info),
                location.getEnd().getAbsolutePosition(info) + 1);
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
