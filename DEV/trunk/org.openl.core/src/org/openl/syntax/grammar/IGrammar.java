/*
 * Created on Jul 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.grammar;

import java.io.Reader;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 * 
 */
public interface IGrammar {

    public SyntaxNodeException[] getErrors();

    public ISyntaxNode getTopNode();

    public void parse(Reader characterStream, String parseType);

    public void parseAsMethod(Reader r);

    public void parseAsMethodHeader(Reader r);

    public void parseAsModule(Reader r);

    public void parseAsType(Reader reader);

    public void setModule(IOpenSourceCodeModule module);

}
