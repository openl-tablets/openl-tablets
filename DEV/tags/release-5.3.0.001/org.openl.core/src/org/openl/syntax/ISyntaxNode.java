/*
 * Created on May 12, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax;

import org.openl.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public interface ISyntaxNode {

    public ISyntaxNode getChild(int i);

    IOpenSourceCodeModule getModule();

    public int getNumberOfChildren();

    ISyntaxNode getParent();

    public ILocation getSourceLocation();

    public String getType();

    /**
     * @param i
     * @param buf
     */
    void print(int i, StringBuffer buf);

    // public Map<String, String> getProperties();

    void setParent(ISyntaxNode node);

    // public String getNamespace();

}
