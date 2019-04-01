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

    SyntaxNodeException[] getErrors();

    ISyntaxNode getTopNode();

    void parse(Reader characterStream, String parseType);

    void parseAsMethod(Reader r);

    void parseAsMethodHeader(Reader r);

    void parseAsModule(Reader r);

    void parseAsType(Reader reader);

    void setModule(IOpenSourceCodeModule module);

}
