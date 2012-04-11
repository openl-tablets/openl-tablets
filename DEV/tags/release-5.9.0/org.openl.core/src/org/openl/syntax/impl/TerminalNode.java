/*
 * Created on May 13, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 * 
 */
public abstract class TerminalNode extends ASyntaxNode {

    public TerminalNode(String type, ILocation location, IOpenSourceCodeModule module) {
        super(type, location, module);
    }

    /*
     * (non-Javadoc)
     * @see org.openl.parser.SyntaxNode#getChild(int)
     */
    public ISyntaxNode getChild(int i) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.parser.SyntaxNode#getNumberOfChildren()
     */
    public int getNumberOfChildren() {
        return 0;
    }

}
