/*
 * Created on Jul 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.grammar;

import java.io.Reader;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 * 
 */
public interface IGrammar {

    public ISyntaxError[] getErrors();

    public ISyntaxNode getTopNode();

    public void parse(Reader characterStream, String parseType);

    public void parseAsMethod(Reader r);

    public void parseAsMethodHeader(Reader r);

    public void parseAsModule(Reader r);

    public void parseAsType(Reader reader);

    public void setModule(IOpenSourceCodeModule module);

}
