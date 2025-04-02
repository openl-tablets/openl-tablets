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
 * <p>The returned location identifies where in the source code the node is defined,
 * allowing for error mapping and source code navigation.</p>
 *
 * @return the location in the source code corresponding to this node
 */
ILocation getSourceLocation();

    /**
 * Retrieves the source code module associated with this syntax node.
 *
 * @return the source code module for this node
 */
IOpenSourceCodeModule getSourceCodeModule();

    /**
 * Returns the type of this syntax node.
 *
 * @return a string representing the node's type
 */
String getType();

    /**
     * Returns the text representation of the syntax node.
     * 
     * <p>This default implementation returns {@code null}, indicating that no textual representation
     * is provided. Implementing classes may override this method to supply the appropriate text.
     *
     * @return the text representation of the node, or {@code null} if not applicable
     */
    default String getText() {
        return null;
    }

    void print(int i, StringBuilder buf);

    void setParent(ISyntaxNode node);

    ISyntaxNode[] EMPTY = {};

}
