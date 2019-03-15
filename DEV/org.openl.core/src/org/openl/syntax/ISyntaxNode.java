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
 *
 */
public interface ISyntaxNode {

    ISyntaxNode getChild(int i);

    IOpenSourceCodeModule getModule();

    int getNumberOfChildren();

    ISyntaxNode getParent();

    ILocation getSourceLocation();

    String getType();

    default String getText() {
        return null;
    }

    void print(int i, StringBuilder buf);

    void setParent(ISyntaxNode node);

    ISyntaxNode[] EMPTY = {};
    
}
