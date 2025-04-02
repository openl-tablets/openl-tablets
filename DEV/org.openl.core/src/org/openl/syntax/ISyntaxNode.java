/*
 * Created on May 12, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 */
public interface ISyntaxNode {

    ISyntaxNode getChild(int i);

    IOpenSourceCodeModule getModule();

    int getNumberOfChildren();

    ISyntaxNode getParent();

    /**
 * Returns the source location of this syntax node.
 *
 * @return an ILocation representing the node's position in the source code
 */
ILocation getSourceLocation();

    /**
 * Returns the source code module associated with this syntax node.
 *
 * @return the source code module related to this syntax node
 */
IOpenSourceCodeModule getSourceCodeModule();

    /**
 * Returns the type of the syntax node.
 *
 * <p>This method provides a string identifier that represents the category or role of the syntax node within the syntax tree.</p>
 *
 * @return a string indicating the syntax node's type
 */
String getType();

    /**
     * Returns the text representation of the syntax node.
     * <p>
     * The default implementation always returns {@code null}, indicating that the syntax node
     * does not provide an associated text representation.
     * </p>
     *
     * @return {@code null} by default.
     */
    default String getText() {
        return null;
    }

    void print(int i, StringBuilder buf);

    void setParent(ISyntaxNode node);

    ISyntaxNode[] EMPTY = {};

}
