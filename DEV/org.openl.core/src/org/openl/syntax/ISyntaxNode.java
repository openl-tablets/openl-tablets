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
 * Retrieves the source location of this syntax node.
 *
 * @return an {@link ILocation} instance representing the position in the source code of this node
 */
ILocation getSourceLocation();

    /**
 * Returns the source code module associated with this syntax node.
 *
 * @return the source code module related to the syntax node
 */
IOpenSourceCodeModule getSourceCodeModule();

    /**
 * Returns the type of the syntax node.
 *
 * @return a string representing the syntax node type
 */
String getType();

    /**
     * Returns the text representation of the syntax node.
     *
     * <p>This default implementation always returns {@code null}, indicating that no text representation is provided.
     *
     * @return {@code null} as there is no text representation available
     */
    default String getText() {
        return null;
    }

    void print(int i, StringBuilder buf);

    void setParent(ISyntaxNode node);

    ISyntaxNode[] EMPTY = {};

}
